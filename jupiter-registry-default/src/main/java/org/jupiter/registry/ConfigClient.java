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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.jupiter.common.concurrent.ConcurrentSet;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Pair;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.transport.Acknowledge;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JOption;
import org.jupiter.transport.JProtocolHeader;
import org.jupiter.transport.error.ConnectFailedException;
import org.jupiter.transport.error.Signal;
import org.jupiter.transport.error.Signals;
import org.jupiter.transport.netty.NettyTcpConnector;
import org.jupiter.transport.netty.channel.NettyChannel;
import org.jupiter.transport.netty.handler.AcknowledgeEncoder;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.connector.ConnectionWatchdog;
import org.jupiter.transport.netty.handler.connector.ConnectorIdleStateTrigger;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jupiter.common.util.JConstants.WRITER_IDLE_TIME_SECONDS;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.registry.RegisterMeta.Address;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;
import static org.jupiter.serialization.SerializerHolder.serializer;
import static org.jupiter.transport.JProtocolHeader.*;
import static org.jupiter.transport.error.Signals.ILLEGAL_MAGIC;
import static org.jupiter.transport.error.Signals.ILLEGAL_SIGN;

/**
 * The client of registration center.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class ConfigClient extends NettyTcpConnector {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConfigClient.class);

    private static final AttributeKey<ConcurrentSet<ServiceMeta>> C_SUBSCRIBE_KEY = AttributeKey.valueOf("C_Subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> C_PUBLISH_KEY = AttributeKey.valueOf("C_Published");

    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<Long, MessageNonAck> messagesNonAck = Maps.newConcurrentHashMap();
    // Consumer订阅信息
    private final ConcurrentSet<ServiceMeta> subscribeSet = new ConcurrentSet<>();
    // Provider注册信息
    private final ConcurrentSet<RegisterMeta> registerMetaSet = new ConcurrentSet<>();

    // handlers
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();

    // 每个ConfigClient只保留一个有效channel
    private volatile Channel channel;

    private AbstractRegistryService registryService;

    public ConfigClient(AbstractRegistryService registryService) {
        this(registryService, 1);
    }

    public ConfigClient(AbstractRegistryService registryService, int nWorkers) {
        super(nWorkers);
        this.registryService = checkNotNull(registryService, "registryService");
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) SECONDS.toMillis(3));
        // channel factory
        bootstrap().channel(NioSocketChannel.class);
    }

    /**
     * ConfigClient不支持异步连接行为, async参数无效
     */
    @Override
    public JConnection connect(UnresolvedAddress remoteAddress, boolean async) {
        setOptions();

        Bootstrap boot = bootstrap();

        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, remoteAddress, null) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new IdleStateChecker(timer, 0, WRITER_IDLE_TIME_SECONDS, 0),
                        idleStateTrigger,
                        new MessageDecoder(),
                        encoder,
                        handler
                };
            }};
        watchdog.setReconnect(true);

        try {
            ChannelFuture future;
            synchronized (bootstrapLock()) {
                boot.handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect(remoteAddress.getHost(), remoteAddress.getPort());
            }

            // 以下代码在synchronized同步块外面是安全的
            future.sync();
            channel = future.channel();
        } catch (Exception e) {
            throw new ConnectFailedException("the connection fails", e);
        }

        return new JConnection(remoteAddress) {

            @Override
            public void setReconnect(boolean reconnect) {
                watchdog.setReconnect(reconnect);
            }
        };
    }

    /**
     * Sent the subscription information to the config server.
     */
    public void doSubscribe(ServiceMeta serviceMeta) {
        subscribeSet.add(serviceMeta);

        Message msg = new Message();
        msg.sign(SUBSCRIBE_SERVICE);
        msg.data(serviceMeta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachSubscribeEventOnChannel(serviceMeta, ch)) {
            ch.writeAndFlush(msg);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    /**
     * Publishing service to the config server.
     */
    public void doRegister(RegisterMeta meta) {
        registerMetaSet.add(meta);

        Message msg = new Message();
        msg.sign(PUBLISH_SERVICE);
        msg.data(meta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachPublishEventOnChannel(meta, ch)) {
            ch.writeAndFlush(msg);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    /**
     * Notification the config server unpublish corresponding service.
     */
    public void doUnregister(RegisterMeta meta) {
        registerMetaSet.remove(meta);

        Message msg = new Message();
        msg.sign(PUBLISH_CANCEL_SERVICE);
        msg.data(meta);

        channel.writeAndFlush(msg);

        MessageNonAck msgNonAck = new MessageNonAck(msg, channel);
        messagesNonAck.put(msgNonAck.id, msgNonAck);
    }

    private void handleAcknowledge(Acknowledge ack) {
        messagesNonAck.remove(ack.sequence());
    }

    // 在channel打标记(发布过的服务)
    private static boolean attachPublishEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(C_PUBLISH_KEY);
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

    // 在channel打标记(订阅过的服务)
    private static boolean attachSubscribeEventOnChannel(ServiceMeta serviceMeta, Channel channel) {
        Attribute<ConcurrentSet<ServiceMeta>> attr = channel.attr(C_SUBSCRIBE_KEY);
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

    static class MessageNonAck {
        private final long id;

        private final Message msg;
        private final Channel channel;
        private final long timestamp = SystemClock.millisClock().now();

        public MessageNonAck(Message msg, Channel channel) {
            this.msg = msg;
            this.channel = channel;

            id = msg.sequence();
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
                        case PUBLISH_SERVICE:
                        case OFFLINE_NOTICE: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Message msg = serializer().readObject(bytes, Message.class);
                            msg.sign(header.sign());
                            out.add(msg);

                            break;
                        }
                        case ACK: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Acknowledge ack = serializer().readObject(bytes, Acknowledge.class);
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
            byte[] bytes = serializer().writeObject(msg);
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

        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Message) {
                Message obj = (Message) msg;

                switch (obj.sign()) {
                    case PUBLISH_SERVICE:
                        Pair<ServiceMeta, List<RegisterMeta>> data = (Pair<ServiceMeta, List<RegisterMeta>>) obj.data();
                        registryService.notify(data.getKey(), data.getValue(), obj.getVersion());

                        ctx.channel().writeAndFlush(new Acknowledge(obj.sequence())); // 回复ACK

                        logger.info("Publish from ConfigServer {}, version: {}.", data.getKey(), obj.getVersion());

                        break;
                    case OFFLINE_NOTICE:
                        Address address = (Address) obj.data();
                        registryService.offline(address);

                        logger.info("Offline notice on {}.", address);

                        break;
                }
            } else if (msg instanceof Acknowledge) {
                handleAcknowledge((Acknowledge) msg);
            } else {
                logger.warn("Unexpected msg type received:{}.", msg.getClass());

                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel ch = (channel = ctx.channel());

            // 重新订阅
            for (ServiceMeta serviceMeta : subscribeSet) {
                // 与doSubscribe()中的write有竞争
                if (!attachSubscribeEventOnChannel(serviceMeta, ch)) {
                    continue;
                }

                Message msg = new Message();
                msg.sign(SUBSCRIBE_SERVICE);
                msg.data(serviceMeta);

                ch.writeAndFlush(msg);

                MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
                messagesNonAck.put(msgNonAck.id, msgNonAck);
            }

            // 重新发布服务
            for (RegisterMeta meta : registerMetaSet) {
                // 与doRegister()中的write有竞争
                if (!attachPublishEventOnChannel(meta, ch)) {
                    continue;
                }

                Message msg = new Message();
                msg.sign(PUBLISH_SERVICE);
                msg.data(meta);

                ch.writeAndFlush(msg);

                MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
                messagesNonAck.put(msgNonAck.id, msgNonAck);
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
                        if (SystemClock.millisClock().now() - m.timestamp > SECONDS.toMillis(10)) {

                            // 移除
                            if (messagesNonAck.remove(m.id) == null) {
                                continue;
                            }

                            if (m.channel.isActive()) {
                                MessageNonAck msgNonAck = new MessageNonAck(m.msg, m.channel);
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
