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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import org.jupiter.common.concurrent.collection.ConcurrentSet;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Pair;
import org.jupiter.common.util.Signal;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.ThrowUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.Acknowledge;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JOption;
import org.jupiter.transport.JProtocolHeader;
import org.jupiter.transport.exception.IoSignals;
import org.jupiter.transport.netty.NettyTcpAcceptor;
import org.jupiter.transport.netty.handler.AcknowledgeEncoder;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;

/**
 * The server of registration center.
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
public final class DefaultRegistryServer extends NettyTcpAcceptor implements RegistryServer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultRegistryServer.class);

    private static final AttributeKey<ConcurrentSet<RegisterMeta.ServiceMeta>> S_SUBSCRIBE_KEY =
            AttributeKey.valueOf("server.subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> S_PUBLISH_KEY =
            AttributeKey.valueOf("server.published");

    // 注册信息
    private final RegisterInfoContext registerInfoContext = new RegisterInfoContext();
    // 订阅者
    private final ChannelGroup subscriberChannels = new DefaultChannelGroup("subscribers", GlobalEventExecutor.INSTANCE);
    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<String, MessageNonAck> messagesNonAck = Maps.newConcurrentMap();

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();

    // 序列化/反序列化方式
    private final SerializerType serializerType;

    {
        SerializerType expected = SerializerType.parse(SystemPropertyUtil.get("jupiter.registry.default.serializer_type"));
        serializerType = expected == null ? SerializerType.getDefault() : expected;
    }

    public DefaultRegistryServer(int port) {
        super(port);
    }

    public DefaultRegistryServer(SocketAddress address) {
        super(address);
    }

    public DefaultRegistryServer(int port, int nWorkers) {
        super(port, nWorkers);
    }

    public DefaultRegistryServer(SocketAddress address, int nWorkers) {
        super(address, nWorkers);
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 32768);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        ServerBootstrap boot = bootstrap();

        initChannelFactory();

        boot.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(
                        new IdleStateChecker(timer, JConstants.READER_IDLE_TIME_SECONDS, 0, 0),
                        idleStateTrigger,
                        new MessageDecoder(),
                        encoder,
                        ackEncoder,
                        handler);
            }
        });

        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    public List<String> listPublisherHosts() {
        List<RegisterMeta.Address> fromList = registerInfoContext.listPublisherHosts();

        return Lists.transform(fromList, RegisterMeta.Address::getHost);
    }

    @Override
    public List<String> listSubscriberAddresses() {
        List<String> hosts = Lists.newArrayList();
        for (Channel ch : subscriberChannels) {
            SocketAddress address = ch.remoteAddress();
            if (address instanceof InetSocketAddress) {
                String host = ((InetSocketAddress) address).getAddress().getHostAddress();
                int port = ((InetSocketAddress) address).getPort();
                hosts.add(new RegisterMeta.Address(host, port).toString());
            }
        }
        return hosts;
    }

    @Override
    public List<String> listAddressesByService(String group, String serviceProviderName, String version) {
        RegisterMeta.ServiceMeta serviceMeta = new RegisterMeta.ServiceMeta(group, serviceProviderName, version);
        List<RegisterMeta.Address> fromList = registerInfoContext.listAddressesByService(serviceMeta);

        return Lists.transform(fromList, RegisterMeta.Address::toString);
    }

    @Override
    public List<String> listServicesByAddress(String host, int port) {
        RegisterMeta.Address address = new RegisterMeta.Address(host, port);
        List<RegisterMeta.ServiceMeta> fromList = registerInfoContext.listServicesByAddress(address);

        return Lists.transform(fromList, RegisterMeta.ServiceMeta::toString);
    }

    @Override
    public void startRegistryServer() {
        try {
            start();
        } catch (InterruptedException e) {
            ThrowUtil.throwException(e);
        }
    }

    // 添加指定机器指定服务, 然后全量发布到所有客户端
    private void handlePublish(RegisterMeta meta, Channel channel) {

        logger.info("Publish {} on channel{}.", meta, channel);

        attachPublishEventOnChannel(meta, channel);

        final RegisterMeta.ServiceMeta serviceMeta = meta.getServiceMeta();
        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            if (config.getConfig().putIfAbsent(meta.getAddress(), meta) == null) {
                registerInfoContext.getServiceMeta(meta.getAddress()).add(serviceMeta);

                final Message msg = new Message(serializerType.value());
                msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
                msg.version(config.newVersion()); // 版本号+1
                msg.data(Pair.of(serviceMeta, meta));

                subscriberChannels.writeAndFlush(msg, ch -> {
                    boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, ch);
                    if (doSend) {
                        MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, ch);
                        // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                        messagesNonAck.put(msgNonAck.id, msgNonAck);
                    }
                    return doSend;
                });
            }
        }
    }

    // 删除指定机器指定服务, 然后全量发布到所有客户端
    private void handlePublishCancel(RegisterMeta meta, Channel channel) {

        logger.info("Cancel publish {} on channel{}.", meta, channel);

        attachPublishCancelEventOnChannel(meta, channel);

        final RegisterMeta.ServiceMeta serviceMeta = meta.getServiceMeta();
        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            RegisterMeta.Address address = meta.getAddress();
            RegisterMeta data = config.getConfig().remove(address);
            if (data != null) {
                registerInfoContext.getServiceMeta(address).remove(serviceMeta);

                final Message msg = new Message(serializerType.value());
                msg.messageCode(JProtocolHeader.PUBLISH_CANCEL_SERVICE);
                msg.version(config.newVersion()); // 版本号+1
                msg.data(Pair.of(serviceMeta, data));

                subscriberChannels.writeAndFlush(msg, ch -> {
                    boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, ch);
                    if (doSend) {
                        MessageNonAck msgNonAck = new MessageNonAck(serviceMeta, msg, ch);
                        // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                        messagesNonAck.put(msgNonAck.id, msgNonAck);
                    }
                    return doSend;
                });
            }
        }
    }

    // 订阅服务
    private void handleSubscribe(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {

        logger.info("Subscribe {} on channel{}.", serviceMeta, channel);

        attachSubscribeEventOnChannel(serviceMeta, channel);

        subscriberChannels.add(channel);

        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        final Message msg = new Message(serializerType.value());
        msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
        msg.version(config.getVersion()); // 版本号
        List<RegisterMeta> registerMetaList = Lists.newArrayList(config.getConfig().values());
        // 每次发布服务都是当前meta的全量信息
        msg.data(Pair.of(serviceMeta, registerMetaList));

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
    private void handleOfflineNotice(RegisterMeta.Address address) {

        logger.info("OfflineNotice on {}.", address);

        Message msg = new Message(serializerType.value());
        msg.messageCode(JProtocolHeader.OFFLINE_NOTICE);
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
    private static boolean attachSubscribeEventOnChannel(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta.ServiceMeta>> attr = channel.attr(S_SUBSCRIBE_KEY);
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = attr.get();
        if (serviceMetaSet == null) {
            ConcurrentSet<RegisterMeta.ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
            serviceMetaSet = attr.setIfAbsent(newServiceMetaSet);
            if (serviceMetaSet == null) {
                serviceMetaSet = newServiceMetaSet;
            }
        }

        return serviceMetaSet.add(serviceMeta);
    }

    // 检查channel上的标记(是否订阅过指定的服务)
    private static boolean isChannelSubscribeOnServiceMeta(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = channel.attr(S_SUBSCRIBE_KEY).get();
        return serviceMetaSet != null && serviceMetaSet.contains(serviceMeta);
    }

    /**
     * 没收到ACK, 需要重发消息
     */
    static class MessageNonAck {
        private final String id;

        private final RegisterMeta.ServiceMeta serviceMeta;
        private final Message msg;
        private final Channel channel;
        private final long version;
        private final long timestamp = SystemClock.millisClock().now();

        public MessageNonAck(RegisterMeta.ServiceMeta serviceMeta, Message msg, Channel channel) {
            this.serviceMeta = serviceMeta;
            this.msg = msg;
            this.channel = channel;
            this.version = msg.version();

            id = key(msg.sequence(), channel);
        }
    }

    /**
     * <pre>
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id    Body Size                    Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // magic = (short) 0xbabe
     * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
     * + 1 // 空
     * + 8 // 消息 id, long 类型
     * + 4 // 消息体 body 长度, int 类型
     * </pre>
     */
    static class MessageDecoder extends ReplayingDecoder<MessageDecoder.State> {

        public MessageDecoder() {
            super(State.MAGIC);
        }

        // 协议头
        private final JProtocolHeader header = new JProtocolHeader();

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            switch (state()) {
                case MAGIC:
                    checkMagic(in.readShort());             // MAGIC
                    checkpoint(State.SIGN);
                case SIGN:
                    header.sign(in.readByte());             // 消息标志位
                    checkpoint(State.STATUS);
                case STATUS:
                    in.readByte();                          // no-op
                    checkpoint(State.ID);
                case ID:
                    header.id(in.readLong());               // 消息id
                    checkpoint(State.BODY_SIZE);
                case BODY_SIZE:
                    header.bodySize(in.readInt());          // 消息体长度
                    checkpoint(State.BODY);
                case BODY:
                    byte s_code = header.serializerCode();

                    switch (header.messageCode()) {
                        case JProtocolHeader.HEARTBEAT:
                            break;
                        case JProtocolHeader.PUBLISH_SERVICE:
                        case JProtocolHeader.PUBLISH_CANCEL_SERVICE:
                        case JProtocolHeader.SUBSCRIBE_SERVICE:
                        case JProtocolHeader.OFFLINE_NOTICE: {
                            byte[] bytes = new byte[header.bodySize()];
                            in.readBytes(bytes);

                            Serializer serializer = SerializerFactory.getSerializer(s_code);
                            Message msg = serializer.readObject(bytes, Message.class);
                            msg.messageCode(header.messageCode());
                            out.add(msg);

                            break;
                        }
                        case JProtocolHeader.ACK:
                            out.add(new Acknowledge(header.id()));

                            break;
                        default:
                            throw IoSignals.ILLEGAL_SIGN;
                    }
                    checkpoint(State.MAGIC);
            }
        }

        private static void checkMagic(short magic) throws Signal {
            if (magic != JProtocolHeader.MAGIC) {
                throw IoSignals.ILLEGAL_MAGIC;
            }
        }

        enum State {
            MAGIC,
            SIGN,
            STATUS,
            ID,
            BODY_SIZE,
            BODY
        }
    }

    /**
     * <pre>
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id    Body Size                    Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // magic = (short) 0xbabe
     * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
     * + 1 // 空
     * + 8 // 消息 id, long 类型
     * + 4 // 消息体 body 长度, int 类型
     * </pre>
     */
    @ChannelHandler.Sharable
    static class MessageEncoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
            byte s_code = msg.serializerCode();
            byte sign = JProtocolHeader.toSign(s_code, msg.messageCode());
            Serializer serializer = SerializerFactory.getSerializer(s_code);
            byte[] bytes = serializer.writeObject(msg);

            out.writeShort(JProtocolHeader.MAGIC)
                    .writeByte(sign)
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
            Channel ch = ctx.channel();

            if (msg instanceof Message) {
                Message obj = (Message) msg;

                switch (obj.messageCode()) {
                    case JProtocolHeader.PUBLISH_SERVICE:
                    case JProtocolHeader.PUBLISH_CANCEL_SERVICE:
                        RegisterMeta meta = (RegisterMeta) obj.data();
                        if (Strings.isNullOrEmpty(meta.getHost())) {
                            SocketAddress address = ch.remoteAddress();
                            if (address instanceof InetSocketAddress) {
                                meta.setHost(((InetSocketAddress) address).getAddress().getHostAddress());
                            } else {
                                logger.warn("Could not get remote host: {}, info: {}", ch, meta);

                                return;
                            }
                        }

                        if (obj.messageCode() == JProtocolHeader.PUBLISH_SERVICE) {
                            handlePublish(meta, ch);
                        } else if (obj.messageCode() == JProtocolHeader.PUBLISH_CANCEL_SERVICE) {
                            handlePublishCancel(meta, ch);
                        }
                        ch.writeAndFlush(new Acknowledge(obj.sequence())) // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        break;
                    case JProtocolHeader.SUBSCRIBE_SERVICE:
                        handleSubscribe((RegisterMeta.ServiceMeta) obj.data(), ch);
                        ch.writeAndFlush(new Acknowledge(obj.sequence())) // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        break;
                    case JProtocolHeader.OFFLINE_NOTICE:
                        handleOfflineNotice((RegisterMeta.Address) obj.data());

                        break;
                }
            } else if (msg instanceof Acknowledge) {
                handleAcknowledge((Acknowledge) msg, ch);
            } else {
                logger.warn("Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);

                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel ch = ctx.channel();

            // 取消之前发布的所有服务
            ConcurrentSet<RegisterMeta> registerMetaSet = ch.attr(S_PUBLISH_KEY).get();

            if (registerMetaSet == null || registerMetaSet.isEmpty()) {
                return;
            }

            RegisterMeta.Address address = null;
            for (RegisterMeta meta : registerMetaSet) {
                if (address == null) {
                    address = meta.getAddress();
                }
                handlePublishCancel(meta, ch);
            }

            if (address != null) {
                // 通知所有订阅者对应机器下线
                handleOfflineNotice(address);
            }
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            Channel ch = ctx.channel();
            ChannelConfig config = ch.config();

            // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
            // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
            if (!ch.isWritable()) {
                // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
                if (logger.isWarnEnabled()) {
                    logger.warn("{} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                            ch, config.getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());
                }

                config.setAutoRead(false);
            } else {
                // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
                if (logger.isWarnEnabled()) {
                    logger.warn("{} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                            ch, config.getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());
                }

                config.setAutoRead(true);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            Channel ch = ctx.channel();

            if (cause instanceof Signal) {
                logger.error("I/O signal was caught: {}, force to close channel: {}.", ((Signal) cause).name(), ch);

                ch.close();
            } else if (cause instanceof IOException) {
                logger.error("I/O exception was caught: {}, force to close channel: {}.",
                        StackTraceUtil.stackTrace(cause), ch);

                ch.close();
            } else if (cause instanceof DecoderException) {
                logger.error("Decoder exception was caught: {}, force to close channel: {}.",
                        StackTraceUtil.stackTrace(cause), ch);

                ch.close();
            } else {
                logger.error("Unexpected exception was caught: {}, channel: {}.", StackTraceUtil.stackTrace(cause), ch);
            }
        }
    }

    @SuppressWarnings("all")
    private class AckTimeoutScanner implements Runnable {

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
                                m.channel.writeAndFlush(m.msg)
                                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                            }
                        }
                    }
                } catch (Throwable t) {
                    logger.error("An exception was caught while scanning the timeout acknowledges {}.",
                            StackTraceUtil.stackTrace(t));
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    {
        Thread t = new Thread(new AckTimeoutScanner(), "ack.timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
