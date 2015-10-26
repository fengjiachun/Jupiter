package org.jupiter.registry;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.*;
import org.jupiter.common.concurrent.ConcurrentSet;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Pair;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.serialization.SerializerHolder;
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
import org.jupiter.transport.netty.handler.ChannelHandlerHolder;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.connector.ConnectorIdleStateTrigger;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.jupiter.common.util.JConstants.WRITER_IDLE_TIME_SECONDS;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.registry.RegisterMeta.*;
import static org.jupiter.transport.JProtocolHeader.*;
import static org.jupiter.transport.error.Signals.ILLEGAL_MAGIC;
import static org.jupiter.transport.error.Signals.ILLEGAL_SIGN;

/**
 * 注册中心客户端
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class ConfigClient extends NettyTcpConnector {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConfigClient.class);

    private static final AttributeKey<ConcurrentSet<ServiceMeta>> SUBSCRIBE_KEY = AttributeKey.valueOf("subscribeKey");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> PUBLISH_KEY = AttributeKey.valueOf("publishKey");

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
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(3));
        // channel factory
        bootstrap().channel(NioSocketChannel.class);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Override
    public JConnection connect(UnresolvedAddress remoteAddress, boolean async) {
        setOptions();

        Bootstrap boot = bootstrap();

        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, remoteAddress) {

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
            synchronized (boot) {
                boot.handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect(remoteAddress.getHost(), remoteAddress.getPort());
            }

            // 以下代码在synchronized同步块外面是安全的
            if (!async) {
                future.sync();
                channel = future.channel();
            }
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

    public void doSubscribe(ServiceMeta serviceMeta) {
        subscribeSet.add(serviceMeta);

        Message msg = new Message();
        msg.sign(SUBSCRIBE_SERVICE);
        msg.data(serviceMeta);

        Channel ch = channel;
        if (attachSubscribeEventOnChannel(serviceMeta, ch)) {
            ch.writeAndFlush(msg);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    public void doRegister(RegisterMeta meta) {
        registerMetaSet.add(meta);

        Message msg = new Message();
        msg.sign(PUBLISH_SERVICE);
        msg.data(meta);

        Channel ch = channel;
        if (attachPublishEventOnChannel(meta, ch)) {
            ch.writeAndFlush(msg);

            MessageNonAck msgNonAck = new MessageNonAck(msg, ch);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    public void doUnregister(RegisterMeta meta) {
        registerMetaSet.remove(meta);

        Message msg = new Message();
        msg.sign(UN_PUBLISH_SERVICE);
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
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(PUBLISH_KEY);
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
        Attribute<ConcurrentSet<ServiceMeta>> attr = channel.attr(SUBSCRIBE_KEY);
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

        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof Message) {
                Message obj = (Message) msg;

                switch (obj.sign()) {
                    case PUBLISH_SERVICE:
                        ctx.channel().writeAndFlush(new Acknowledge(obj.sequence())); // 回复ACK

                        Pair<ServiceMeta, List<RegisterMeta>> data = (Pair<ServiceMeta, List<RegisterMeta>>) obj.data();

                        registryService.notify(data.getKey(), data.getValue(), obj.getVersion());

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
                if (attachPublishEventOnChannel(meta, ch)) {
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

    @ChannelHandler.Sharable
    public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {
        private final Bootstrap bootstrap;
        private final Timer timer;
        private final UnresolvedAddress remoteAddress;

        private volatile boolean reconnect = true;
        private int attempts;

        public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, UnresolvedAddress remoteAddress) {
            this.bootstrap = bootstrap;
            this.timer = timer;
            this.remoteAddress = remoteAddress;
        }

        public void setReconnect(boolean reconnect) {
            this.reconnect = reconnect;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            Channel ch = ctx.channel();

            logger.error("An exception has been caught {}, on {}.", stackTrace(cause), ch);

            ch.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            attempts = 0;

            logger.info("Connects with {}.", ctx.channel());

            ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            boolean doReconnect = reconnect;
            if (doReconnect) {
                if (attempts < 12) {
                    attempts++;
                }
                int timeout = 2 << attempts;
                timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
            }

            logger.warn("Disconnects with {}, address: [{}:{}], reconnect: {}.",
                    ctx.channel(), remoteAddress.getHost(), remoteAddress.getPort(), doReconnect);

            ctx.fireChannelInactive();
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            ChannelFuture future;
            final String host = remoteAddress.getHost();
            final int port = remoteAddress.getPort();
            synchronized (bootstrap) {
                bootstrap.handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(handlers());
                    }
                });
                future = bootstrap.connect(host, port);
            }

            future.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    boolean succeed = f.isSuccess();
                    Channel ch = f.channel();

                    logger.warn("Reconnects with [{}:{}] {}.", host, port, succeed ? "succeed" : "failed");

                    if (!succeed) {
                        ch.pipeline().fireChannelInactive();
                    }
                }
            });
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
