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
import org.jupiter.common.concurrent.collection.ConcurrentSet;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Pair;
import org.jupiter.common.util.Signal;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.transport.*;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.exception.IoSignals;
import org.jupiter.transport.netty.NettyTcpConnector;
import org.jupiter.transport.netty.TcpChannelProvider;
import org.jupiter.transport.netty.channel.NettyChannel;
import org.jupiter.transport.netty.handler.AcknowledgeEncoder;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.connector.ConnectionWatchdog;
import org.jupiter.transport.netty.handler.connector.ConnectorIdleStateTrigger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jupiter.common.util.JConstants.WRITER_IDLE_TIME_SECONDS;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.registry.NotifyListener.NotifyEvent.CHILD_ADDED;
import static org.jupiter.registry.NotifyListener.NotifyEvent.CHILD_REMOVED;
import static org.jupiter.registry.RegisterMeta.Address;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;
import static org.jupiter.serialization.SerializerType.PROTO_STUFF;
import static org.jupiter.transport.JProtocolHeader.*;
import static org.jupiter.transport.exception.IoSignals.ILLEGAL_MAGIC;
import static org.jupiter.transport.exception.IoSignals.ILLEGAL_SIGN;

/**
 * The client of registration center.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class DefaultRegistry extends NettyTcpConnector {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultRegistry.class);

    private static final AttributeKey<ConcurrentSet<ServiceMeta>> C_SUBSCRIBE_KEY = AttributeKey.valueOf("client.subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> C_PUBLISH_KEY = AttributeKey.valueOf("client.published");

    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<Long, MessageNonAck> messagesNonAck = Maps.newConcurrentMap();

    // handlers
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final MessageHandler handler = new MessageHandler();
    private final MessageEncoder encoder = new MessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();

    // 每个ConfigClient只保留一个有效channel
    private volatile Channel channel;

    private AbstractRegistryService registryService;

    public DefaultRegistry(AbstractRegistryService registryService) {
        this(registryService, 1);
    }

    public DefaultRegistry(AbstractRegistryService registryService, int nWorkers) {
        super(nWorkers);
        this.registryService = checkNotNull(registryService, "registryService");
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) SECONDS.toMillis(3));
        // channel factory
        bootstrap().channelFactory(TcpChannelProvider.NIO_CONNECTOR);
    }

    /**
     * ConfigClient不支持异步连接行为, async参数无效
     */
    @Override
    public JConnection connect(UnresolvedAddress address, boolean async) {
        setOptions();

        final Bootstrap boot = bootstrap();
        final SocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());

        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, socketAddress, null) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new IdleStateChecker(timer, 0, WRITER_IDLE_TIME_SECONDS, 0),
                        idleStateTrigger,
                        new MessageDecoder(),
                        encoder,
                        ackEncoder,
                        handler
                };
            }};
        watchdog.start();

        try {
            ChannelFuture future;
            synchronized (bootstrapLock()) {
                boot.handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect(socketAddress);
            }

            // 以下代码在synchronized同步块外面是安全的
            future.sync();
            channel = future.channel();
        } catch (Throwable t) {
            throw new ConnectFailedException("connects to [" + address + "] fails", t);
        }

        return new JConnection(address) {

            @Override
            public void setReconnect(boolean reconnect) {
                if (reconnect) {
                    watchdog.start();
                } else {
                    watchdog.stop();
                }
            }
        };
    }

    /**
     * Sent the subscription information to registry server.
     */
    public void doSubscribe(ServiceMeta serviceMeta) {
        registryService.subscribeSet().add(serviceMeta);

        Message msg = new Message(PROTO_STUFF.value());
        msg.messageCode(SUBSCRIBE_SERVICE);
        msg.data(serviceMeta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachSubscribeEventOnChannel(serviceMeta, ch)) {
            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    /**
     * Publishing service to registry server.
     */
    public void doRegister(RegisterMeta meta) {
        registryService.registerMetaSet().add(meta);

        Message msg = new Message(PROTO_STUFF.value());
        msg.messageCode(PUBLISH_SERVICE);
        msg.data(meta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachPublishEventOnChannel(meta, ch)) {
            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    /**
     * Notify to registry server unpublish corresponding service.
     */
    public void doUnregister(final RegisterMeta meta) {
        registryService.registerMetaSet().remove(meta);

        Message msg = new Message(PROTO_STUFF.value());
        msg.messageCode(PUBLISH_CANCEL_SERVICE);
        msg.data(meta);

        channel.writeAndFlush(msg)
                .addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            Channel ch = future.channel();
                            if (ch.isActive()) {
                                ch.pipeline().fireExceptionCaught(future.cause());
                            } else {
                                logger.warn("Unregister {} fail because of channel is inactive: {}.", meta, stackTrace(future.cause()));
                            }
                        }
                    }
                });

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
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
     * + 1 // 空
     * + 8 // 消息 id, long 类型
     * + 4 // 消息体 body 长度, int类型
     */
    static class MessageDecoder extends ReplayingDecoder<MessageDecoder.State> {

        public MessageDecoder() {
            super(State.HEADER_MAGIC);
        }

        // 协议头
        private final JProtocolHeader header = new JProtocolHeader();

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            switch (state()) {
                case HEADER_MAGIC:
                    checkMagic(in.readShort());             // MAGIC
                    checkpoint(State.HEADER_SIGN);
                case HEADER_SIGN:
                    header.sign(in.readByte());             // 消息标志位
                    checkpoint(State.HEADER_STATUS);
                case HEADER_STATUS:
                    in.readByte();                          // no-op
                    checkpoint(State.HEADER_ID);
                case HEADER_ID:
                    header.id(in.readLong());               // 消息id
                    checkpoint(State.HEADER_BODY_LENGTH);
                case HEADER_BODY_LENGTH:
                    header.bodyLength(in.readInt());        // 消息体长度
                    checkpoint(State.BODY);
                case BODY:
                    byte s_code = header.serializerCode();

                    switch (header.messageCode()) {
                        case PUBLISH_SERVICE:
                        case PUBLISH_CANCEL_SERVICE:
                        case OFFLINE_NOTICE: {
                            byte[] bytes = new byte[header.bodyLength()];
                            in.readBytes(bytes);

                            Serializer serializer = SerializerFactory.getSerializer(s_code);
                            Message msg = serializer.readObject(bytes, Message.class);
                            msg.messageCode(header.messageCode());
                            out.add(msg);

                            break;
                        }
                        case ACK:
                            out.add(new Acknowledge(header.id()));

                            break;
                        default:
                            throw ILLEGAL_SIGN;

                    }
                    checkpoint(State.HEADER_MAGIC);
            }
        }

        private static void checkMagic(short magic) throws Signal {
            if (magic != MAGIC) {
                throw ILLEGAL_MAGIC;
            }
        }

        enum State {
            HEADER_MAGIC,
            HEADER_SIGN,
            HEADER_STATUS,
            HEADER_ID,
            HEADER_BODY_LENGTH,
            BODY
        }
    }

    /**
     * **************************************************************************************************
     *                                          Protocol
     *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
     *       2   │   1   │    1   │     8     │      4      │
     *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
     *           │       │        │           │             │
     *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
     *           │       │        │           │             │
     *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
     *
     * 消息头16个字节定长
     * = 2 // MAGIC = (short) 0xbabe
     * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
     * + 1 // 空
     * + 8 // 消息 id, long 类型
     * + 4 // 消息体 body 长度, int类型
     */
    @ChannelHandler.Sharable
    static class MessageEncoder extends MessageToByteEncoder<Message> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
            byte s_code = msg.serializerCode();
            byte sign = (byte) ((s_code << 4) + msg.messageCode());
            Serializer serializer = SerializerFactory.getSerializer(s_code);
            byte[] bytes = serializer.writeObject(msg);

            out.writeShort(MAGIC)
                    .writeByte(sign)
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

                switch (obj.messageCode()) {
                    case PUBLISH_SERVICE: {
                        Pair<ServiceMeta, ?> data = (Pair<ServiceMeta, ?>) obj.data();
                        Object metaObj = data.getValue();

                        if (metaObj instanceof List) {
                            List<RegisterMeta> list = (List<RegisterMeta>) metaObj;
                            RegisterMeta[] array = new RegisterMeta[list.size()];
                            list.toArray(array);
                            registryService.notify(data.getKey(), CHILD_ADDED, obj.version(), array);
                        } else if (metaObj instanceof RegisterMeta) {
                            registryService.notify(data.getKey(), CHILD_ADDED, obj.version(), (RegisterMeta) metaObj);
                        }

                        ctx.channel()
                                .writeAndFlush(new Acknowledge(obj.sequence()))  // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        logger.info("Publish from RegistryServer {}, metadata : {}, version: {}.",
                                data.getKey(), metaObj, obj.version());

                        break;
                    }
                    case PUBLISH_CANCEL_SERVICE: {
                        Pair<ServiceMeta, RegisterMeta> data = (Pair<ServiceMeta, RegisterMeta>) obj.data();
                        registryService.notify(data.getKey(), CHILD_REMOVED, obj.version(), data.getValue());

                        ctx.channel()
                                .writeAndFlush(new Acknowledge(obj.sequence()))  // 回复ACK
                                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                        logger.info("Publish cancel from RegistryServer {}, metadata : {}, version: {}.",
                                data.getKey(), data.getValue(), obj.version());

                        break;
                    }
                    case OFFLINE_NOTICE:
                        Address address = (Address) obj.data();

                        logger.info("Offline notice on {}.", address);

                        registryService.offline(address);

                        break;
                }
            } else if (msg instanceof Acknowledge) {
                handleAcknowledge((Acknowledge) msg);
            } else {
                logger.warn("Unexpected msg type received: {}.", msg.getClass());

                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel ch = (channel = ctx.channel());

            // 重新订阅
            for (ServiceMeta serviceMeta : registryService.subscribeSet()) {
                // 与doSubscribe()中的write有竞争
                if (!attachSubscribeEventOnChannel(serviceMeta, ch)) {
                    continue;
                }

                Message msg = new Message(PROTO_STUFF.value());
                msg.messageCode(SUBSCRIBE_SERVICE);
                msg.data(serviceMeta);

                ch.writeAndFlush(msg)
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
                messagesNonAck.put(msgNonAck.id, msgNonAck);
            }

            // 重新发布服务
            for (RegisterMeta meta : registryService.registerMetaSet()) {
                // 与doRegister()中的write有竞争
                if (!attachPublishEventOnChannel(meta, ch)) {
                    continue;
                }

                Message msg = new Message(PROTO_STUFF.value());
                msg.messageCode(PUBLISH_SERVICE);
                msg.data(meta);

                ch.writeAndFlush(msg)
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
                messagesNonAck.put(msgNonAck.id, msgNonAck);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
            if (cause instanceof Signal) {
                IoSignals.handleSignal((Signal) cause, jChannel);
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
                                m.channel.writeAndFlush(m.msg)
                                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                            }
                        }
                    }

                    Thread.sleep(300);
                } catch (Throwable t) {
                    logger.error("An exception has been caught while scanning the timeout acknowledges {}.", t);
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
