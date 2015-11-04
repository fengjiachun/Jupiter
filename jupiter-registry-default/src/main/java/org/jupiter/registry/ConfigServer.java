/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.registry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jupiter.common.concurrent.ConcurrentSet;
import org.jupiter.common.util.*;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.serialization.SerializerHolder;
import org.jupiter.transport.Acknowledge;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JOption;
import org.jupiter.transport.JProtocolHeader;
import org.jupiter.transport.error.Signal;
import org.jupiter.transport.error.Signals;
import org.jupiter.transport.netty.NettyTcpAcceptor;
import org.jupiter.transport.netty.channel.NettyChannel;
import org.jupiter.transport.netty.handler.AcknowledgeEncoder;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.jupiter.common.util.JConstants.READER_IDLE_TIME_SECONDS;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.registry.RegisterMeta.Address;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;
import static org.jupiter.transport.JProtocolHeader.*;
import static org.jupiter.transport.error.Signals.ILLEGAL_MAGIC;
import static org.jupiter.transport.error.Signals.ILLEGAL_SIGN;

/**
 * 注册中心服务端
 *
 * 所有信息均在内存中, 不持久化.
 *
 * provider(client)断线时所有该provider发布过的服务会被server清除并通知订阅者, 重新建立连接后provider会自动重新发布相关服务,
 * 并且server会重新推送服务给订阅者.
 *
 * consumer(client)断线时所有该consumer订阅过的服务会被server清除, 重新建立连接后consumer会自动重新订阅相关服务.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class ConfigServer extends NettyTcpAcceptor implements RegistryMonitor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConfigServer.class);

    private static final AttributeKey<ConcurrentSet<ServiceMeta>> S_SUBSCRIBE_KEY = AttributeKey.valueOf("S_Subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> S_PUBLISH_KEY = AttributeKey.valueOf("S_Published");

    // 注册信息
    private final RegisterInfoContext registerInfoContext = new RegisterInfoContext();
    // 订阅者
    private final ChannelGroup subscriberChannels = new DefaultChannelGroup("subscribers", GlobalEventExecutor.INSTANCE);
    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<String, MessageNonAck> messagesNonAck = Maps.newConcurrentHashMap();

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();

    public ConfigServer(int port) {
        super(port, false);
    }

    public ConfigServer(SocketAddress address) {
        super(address, false);
    }

    public ConfigServer(int port, int nWorks) {
        super(port, nWorks, false);
    }

    public ConfigServer(SocketAddress address, int nWorks) {
        super(address, nWorks, false);
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 1024);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress address) {
        ServerBootstrap boot = bootstrap();

        boot.channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new IdleStateChecker(timer, READER_IDLE_TIME_SECONDS, 0, 0),
                                idleStateTrigger,
                                new MessageDecoder(),
                                encoder,
                                ackEncoder,
                                handler);
                    }
                });

        setOptions();

        return boot.bind(address);
    }

    @Override
    public List<String> listPublisherHosts() {
        List<Address> fromList = registerInfoContext.listPublisherHosts();

        return Lists.transform(fromList, new Function<Address, String>() {

            @Override
            public String apply(Address input) {
                return input.getHost();
            }
        });
    }

    @Override
    public List<String> listSubscriberAddresses() {
        List<String> hosts = Lists.newArrayList();
        for (Channel ch : subscriberChannels) {
            SocketAddress address = ch.remoteAddress();
            if (address instanceof InetSocketAddress) {
                String host = ((InetSocketAddress) address).getAddress().getHostAddress();
                int port = ((InetSocketAddress) address).getPort();
                hosts.add(new Address(host, port).toString());
            }
        }
        return hosts;
    }

    @Override
    public List<String> listAddressesByService(String group, String version, String serviceProviderName) {
        ServiceMeta serviceMeta = new ServiceMeta(group, version, serviceProviderName);
        List<Address> fromList = registerInfoContext.listAddressesByService(serviceMeta);

        return Lists.transform(fromList, new Function<Address, String>() {

            @Override
            public String apply(Address input) {
                return input.toString();
            }
        });
    }

    @Override
    public List<String> listServicesByAddress(String host, int port) {
        Address address = new Address(host, port);
        List<ServiceMeta> fromList = registerInfoContext.listServicesByAddress(address);

        return Lists.transform(fromList, new Function<ServiceMeta, String>() {

            @Override
            public String apply(ServiceMeta input) {
                return input.toString();
            }
        });
    }

    // 添加指定机器指定服务, 然后全量发布到所有客户端
    private void handlePublish(RegisterMeta meta, Channel channel) {

        logger.info("Publish {} on channel{}.", meta, channel);

        attachPublishEventOnChannel(meta, channel);

        final ServiceMeta serviceMeta = meta.getServiceMeta();
        ConfigWithVersion<ConcurrentMap<Address, RegisterMeta>> config = registerInfoContext.getRegisterMeta(serviceMeta);

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            if (config.getConfig().putIfAbsent(meta.getAddress(), meta) == null) {
                registerInfoContext.getServiceMeta(meta.getAddress()).add(serviceMeta);

                final Message msg = new Message();
                msg.sign(PUBLISH_SERVICE);
                msg.setVersion(config.newVersion()); // 版本号+1
                List<RegisterMeta> registerMetaList = Lists.newArrayList(config.getConfig().values());
                // 每次发布服务都是当前meta的全量信息
                msg.data(new Pair<>(serviceMeta, registerMetaList));

                subscriberChannels.writeAndFlush(msg, new ChannelMatcher() {

                    @Override
                    public boolean matches(Channel channel) {
                        boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, channel);
                        if (doSend) {
                            MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, channel);
                            // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                            messagesNonAck.put(msgNonAck.id, msgNonAck);
                        }
                        return doSend;
                    }
                });
            }
        }
    }

    // 删除指定机器指定服务, 然后全量发布到所有客户端
    private void handlePublishCancel(RegisterMeta meta, Channel channel) {

        logger.info("Cancel publish {} on channel{}.", meta, channel);

        attachPublishCancelEventOnChannel(meta, channel);

        final ServiceMeta serviceMeta = meta.getServiceMeta();
        ConfigWithVersion<ConcurrentMap<Address, RegisterMeta>> config = registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            Address address = meta.getAddress();
            RegisterMeta data = config.getConfig().remove(address);
            if (data != null) {
                registerInfoContext.getServiceMeta(address).remove(serviceMeta);

                final Message msg = new Message();
                msg.sign(PUBLISH_SERVICE);
                msg.setVersion(config.newVersion()); // 版本号+1
                List<RegisterMeta> registerMetaList = Lists.newArrayList(config.getConfig().values());
                // 每次发布服务都是当前meta的全量信息
                msg.data(new Pair<>(serviceMeta, registerMetaList));

                subscriberChannels.writeAndFlush(msg, new ChannelMatcher() {

                    @Override
                    public boolean matches(Channel channel) {
                        boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, channel);
                        if (doSend) {
                            MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, channel);
                            // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                            messagesNonAck.put(msgNonAck.id, msgNonAck);
                        }
                        return doSend;
                    }
                });
            }
        }
    }

    // 订阅服务
    private void handleSubscribe(ServiceMeta serviceMeta, Channel channel) {

        logger.info("Subscribe {} on channel{}.", serviceMeta, channel);

        attachSubscribeEventOnChannel(serviceMeta, channel);

        subscriberChannels.add(channel);

        ConfigWithVersion<ConcurrentMap<Address, RegisterMeta>> config = registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        final Message msg = new Message();
        msg.sign(PUBLISH_SERVICE);
        msg.setVersion(config.getVersion()); // 版本号
        List<RegisterMeta> registerMetaList = Lists.newArrayList(config.getConfig().values());
        // 每次发布服务都是当前meta的全量信息
        msg.data(new Pair<>(serviceMeta, registerMetaList));

        MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, channel);
        // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
        messagesNonAck.put(msgNonAck.id, msgNonAck);
        channel.writeAndFlush(msg);
    }

    // 处理ack
    private void handleAcknowledge(Acknowledge ack, Channel channel) {
        messagesNonAck.remove(key(ack.sequence(), channel));
    }

    // 发布Provider下线的通告
    private void handleOfflineNotice(Address address) {

        logger.info("OfflineNotice on {}.", address);

        Message msg = new Message();
        msg.sign(OFFLINE_NOTICE);
        msg.data(address);
        subscriberChannels.writeAndFlush(msg);
    }

    private static String key(long sequence, Channel channel) {
        return String.valueOf(sequence) + '-' + channel.id().asShortText();
    }

    // 在channel打标记(发布过的服务)
    private static boolean attachPublishEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(S_PUBLISH_KEY);
        ConcurrentSet<RegisterMeta> registerMetaSet = attr.get();
        if (registerMetaSet == null) {
            ConcurrentSet<RegisterMeta> newRegisterMetaSet = new ConcurrentSet<>();
            registerMetaSet = attr.setIfAbsent(newRegisterMetaSet);
            if (registerMetaSet == null) {
                registerMetaSet = newRegisterMetaSet;
            }
        }

        return registerMetaSet.add(meta);
    }

    // 取消在channel的标记(发布过的服务)
    private static boolean attachPublishCancelEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(S_PUBLISH_KEY);
        ConcurrentSet<RegisterMeta> registerMetaSet = attr.get();
        if (registerMetaSet == null) {
            ConcurrentSet<RegisterMeta> newRegisterMetaSet = new ConcurrentSet<>();
            registerMetaSet = attr.setIfAbsent(newRegisterMetaSet);
            if (registerMetaSet == null) {
                registerMetaSet = newRegisterMetaSet;
            }
        }

        return registerMetaSet.remove(meta);
    }

    // 在channel打标记(订阅过的服务)
    private static boolean attachSubscribeEventOnChannel(ServiceMeta serviceMeta, Channel channel) {
        Attribute<ConcurrentSet<ServiceMeta>> attr = channel.attr(S_SUBSCRIBE_KEY);
        ConcurrentSet<ServiceMeta> serviceMetaSet = attr.get();
        if (serviceMetaSet == null) {
            ConcurrentSet<ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
            serviceMetaSet = attr.setIfAbsent(newServiceMetaSet);
            if (serviceMetaSet == null) {
                serviceMetaSet = newServiceMetaSet;
            }
        }

        return serviceMetaSet.add(serviceMeta);
    }

    // 检查channel上的标记(是否订阅过指定的服务)
    private static boolean isChannelSubscribeOnServiceMeta(ServiceMeta serviceMeta, Channel channel) {
        ConcurrentSet<ServiceMeta> serviceMetaSet = channel.attr(S_SUBSCRIBE_KEY).get();

        return serviceMetaSet != null && serviceMetaSet.contains(serviceMeta);
    }

    /**
     * 没收到ACK, 需要重发消息
     */
    static class MessageNonAck {
        private final String id;

        private final ServiceMeta serviceMeta;
        private final Message msg;
        private final Channel channel;
        private final long version;
        private final long timestamp = SystemClock.millisClock().now();

        public MessageNonAck(ServiceMeta serviceMeta, Message msg, Channel channel) {
            this.serviceMeta = serviceMeta;
            this.msg = msg;
            this.channel = channel;
            this.version = msg.getVersion();

            id = key(msg.sequence(), channel);
        }
    }

    /**
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 用来表示消息类型
     *  +1 // 空
     * + 8 // 消息 id long 类型
     * + 4 // 消息体body长度, int类型
     */
    static class MessageDecoder extends ReplayingDecoder<MessageDecoder.State> {

        public MessageDecoder() {
            super(State.HEADER);
        }

        // 协议头
        private final JProtocolHeader header = new JProtocolHeader();

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            Channel ch = ctx.channel();

            switch (state()) {
                case HEADER:
                    ByteBuf buf = in.readSlice(HEAD_LENGTH);

                    if (MAGIC != buf.readShort()) {          // MAGIC
                        throw ILLEGAL_MAGIC;
                    }

                    header.sign(buf.readByte());            // 消息标志位
                    buf.readByte();                         // noOp
                    header.id(buf.readLong());              // 消息id
                    header.bodyLength(buf.readInt());       // 消息体长度

                    checkpoint(State.BODY);
                case BODY:
                    switch (header.sign()) {
                        case HEARTBEAT:
                            logger.debug("Heartbeat on channel {}.", ch);

                            break;
                        case PUBLISH_SERVICE:
                        case PUBLISH_CANCEL_SERVICE:
                        case SUBSCRIBE_SERVICE:
                        case OFFLINE_NOTICE: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Message msg = SerializerHolder.serializer().readObject(bytes, Message.class);
                            msg.sign(header.sign());
                            out.add(msg);

                            break;
                        }
                        case ACK: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Acknowledge ack = SerializerHolder.serializer().readObject(bytes, Acknowledge.class);
                            out.add(ack);

                            break;
                        }
                        default:
                            throw ILLEGAL_SIGN;
                    }
                    checkpoint(State.HEADER);
            }
        }

        enum State {
            HEADER,
            BODY
        }
    }

    /**
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 用来表示消息类型
     *  +1 // 空
     * + 8 // 消息 id long 类型
     * + 4 // 消息体body长度, int类型
     */
    @ChannelHandler.Sharable
    static class MessageEncoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
            byte[] bytes = SerializerHolder.serializer().writeObject(msg);
            out.writeShort(MAGIC)
                    .writeByte(msg.sign())
                    .writeByte(0)
                    .writeLong(0)
                    .writeInt(bytes.length)
                    .writeBytes(bytes);
        }
    }

    @ChannelHandler.Sharable
    class MessageHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel channel = ctx.channel();

            if (msg instanceof Message) {
                Message obj = (Message) msg;
                switch (obj.sign()) {
                    case PUBLISH_SERVICE:
                    case PUBLISH_CANCEL_SERVICE:
                        channel.writeAndFlush(new Acknowledge(obj.sequence())); // 回复ACK

                        RegisterMeta meta = (RegisterMeta) obj.data();
                        if (Strings.isEmpty(meta.getHost())) {
                            SocketAddress address = channel.remoteAddress();
                            if (address instanceof InetSocketAddress) {
                                meta.setHost(((InetSocketAddress) address).getAddress().getHostAddress());
                            } else {
                                logger.warn("Could not get remote host: {}, info: {}", channel, meta);

                                return;
                            }
                        }

                        if (obj.sign() == PUBLISH_SERVICE) {
                            handlePublish(meta, channel);
                        } else if (obj.sign() == PUBLISH_CANCEL_SERVICE) {
                            handlePublishCancel(meta, channel);
                        }

                        break;
                    case SUBSCRIBE_SERVICE:
                        channel.writeAndFlush(new Acknowledge(obj.sequence())); // 回复ACK

                        handleSubscribe((ServiceMeta) obj.data(), channel);

                        break;
                    case OFFLINE_NOTICE:
                        handleOfflineNotice((Address) obj.data());

                        break;
                }
            } else if (msg instanceof Acknowledge) {
                handleAcknowledge((Acknowledge) msg, channel);
            } else {
                logger.warn("Unexpected msg type received:{}.", msg.getClass());

                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();

            // 取消之前发布的所有服务
            ConcurrentSet<RegisterMeta> registerMetaSet = channel.attr(S_PUBLISH_KEY).get();

            if (registerMetaSet == null) {
                return;
            }

            Address address = null;
            for (RegisterMeta meta : registerMetaSet) {
                if (address == null) {
                    address = meta.getAddress();
                }
                handlePublishCancel(meta, channel);
            }

            // 通知所有订阅者对应机器下线
            handleOfflineNotice(address);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            Channel ch = ctx.channel();

            /**
             * 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK (默认值 64 * 1024)
             * 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK (默认值 32 * 1024)
             */
            if (!ch.isWritable()) {
                // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
                logger.warn("{} is not writable, outbound buffer size: {}.", ch, ch.unsafe().outboundBuffer().size());
            } else {
                // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
                logger.info("{} is writable.", ch);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
            if (cause instanceof Signal) {
                Signals.handleSignal((Signal) cause, jChannel);
            } else {
                logger.error("An exception has been caught {}, on {}.", stackTrace(cause), jChannel);
            }
        }
    }

    private class AckTimeoutScanner implements Runnable {

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            for (;;) {
                try {
                    for (MessageNonAck m : messagesNonAck.values()) {
                        if (SystemClock.millisClock().now() - m.timestamp > TimeUnit.SECONDS.toMillis(10)) {

                            // 移除
                            if (messagesNonAck.remove(m.id) == null) {
                                continue;
                            }

                            if (registerInfoContext.getRegisterMeta(m.serviceMeta).getVersion() > m.version) {
                                // 旧版本的内容不需要重发
                                continue;
                            }

                            if (m.channel.isActive()) {
                                MessageNonAck msgNonAck = new MessageNonAck(m.serviceMeta, m.msg, m.channel);
                                messagesNonAck.put(msgNonAck.id, msgNonAck);
                                m.channel.writeAndFlush(m);
                            }
                        }
                    }

                    Thread.sleep(300);
                } catch (Exception e) {
                    logger.error("An exception has been caught while scanning the timeout acknowledges {}.", e);
                }
            }
        }
    }

    {
        Thread t = new Thread(new AckTimeoutScanner(), "ack.timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
