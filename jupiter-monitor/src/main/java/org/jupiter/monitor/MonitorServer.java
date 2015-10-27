package org.jupiter.monitor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.jupiter.common.util.MD5Util;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.netty.NettyTcpAcceptor;

import java.net.SocketAddress;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static org.jupiter.common.util.JConstants.NEWLINE;
import static org.jupiter.common.util.JConstants.UTF8;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.monitor
 *
 * @author jiachun.fjc
 */
public class MonitorServer extends NettyTcpAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MonitorServer.class);

    private static final AttributeKey<Object> AUTH_KEY = AttributeKey.valueOf("Auth");
    private static final Object AUTH_OBJECT = new Object();
    private static final String DEFAULT_PASSWORD = "e10adc3949ba59abbe56e057f20f883e";

    // handlers
    private final CommandHandler handler = new CommandHandler();
    private final StringEncoder encoder = new StringEncoder(UTF8);

    public MonitorServer(int port) {
        super(port, 1, false);
    }

    @Override
    public ChannelFuture bind(SocketAddress address) {
        ServerBootstrap boot = bootstrap();

        boot.channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new StringDecoder(UTF8),
                                encoder,
                                handler);
                    }
                });

        setOptions();

        return boot.bind(address);
    }

    @Override
    public void start() throws InterruptedException {
        super.start(false);
    }

    private static boolean checkAuth(Channel channel) {
        if (channel.attr(AUTH_KEY).get() == null) {
            channel.writeAndFlush("Permission denied" + NEWLINE).addListener(CLOSE);
            return false;
        }
        return true;
    }

    @ChannelHandler.Sharable
    class CommandHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof String) {
                String[] args = Strings.split(((String) msg).replace("\r\n", ""), ' ');
                Command command = Command.parse(args[0]);
                if (command == null) {
                    ctx.writeAndFlush("invalid command" + NEWLINE);
                    return;
                }

                switch (command) {
                    case AUTH:
                        String password = SystemPropertyUtil.get("monitor.server.password");
                        if (password == null) {
                            password = DEFAULT_PASSWORD;
                        }

                        if (password.equals(MD5Util.getMD5(args[1]))) {
                            ctx.channel().attr(AUTH_KEY).setIfAbsent(AUTH_OBJECT);
                        } else {
                            ctx.writeAndFlush("Permission denied" + NEWLINE).addListener(CLOSE);
                        }

                        break;
                    case HELP:
                        for (Command parent : Command.values()) {
                            StringBuilder buf = new StringBuilder();
                            buf.append(String.format("%1$-32s", parent.name()))
                                    .append(parent.description())
                                    .append(NEWLINE);

                            for (Command.ChildCommand child : parent.children()) {
                                buf.append("                                  - ")
                                        .append(child.name())
                                        .append(' ')
                                        .append(child.description())
                                        .append(NEWLINE);
                            }
                            ctx.writeAndFlush(buf.toString());
                        }

                        break;
                    case METRICS:
                        if (checkAuth(ctx.channel())) {
                            Command.ChildCommand child = command.parseChild(args[1]);
                            if (child != null) {
                                switch (child) {
                                    case REPORT:
                                        // TODO
                                        break;
                                }
                            } else {
                                ctx.writeAndFlush("Wrong args denied" + NEWLINE).addListener(CLOSE);
                            }
                        }

                        break;
                    case QUIT:
                        ctx.writeAndFlush("Bye bye!" + NEWLINE).addListener(CLOSE);

                        break;
                }
            } else {
                logger.warn("Unexpected msg type received:{}.", msg.getClass());

                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("An exception has been caught {}, on {}.", stackTrace(cause), ctx.channel());

            ctx.close();
        }
    }
}
