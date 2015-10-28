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
import org.jupiter.monitor.metric.MetricsReporter;
import org.jupiter.registry.RegistryMonitor;
import org.jupiter.transport.netty.NettyTcpAcceptor;

import java.net.SocketAddress;
import java.util.List;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static org.jupiter.common.util.JConstants.NEWLINE;
import static org.jupiter.common.util.JConstants.UTF8;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * 监控服务, ConfigServer与ProviderServer都应该启用
 *
 * 常用的monitor command说明:
 * ---------------------------------------------------------------------------------------------------------------------
 * help                                 // 帮助信息
 *
 * auth 123456                          // 登录(默认密码为123456,
 *                                      // 可通过System.setProperty("monitor.server.password", "password")设置密码
 *
 * metrics -report                      // 输出当前节点所有指标度量信息
 *
 * registry -address -p                 // 输出所有provider地址
 * registry -address -s                 // 输出所有consumer地址
 * registry -by_service                 // 根据服务(group version providerServiceName)查找所有提供当前服务的机器地址列表
 * registry -by_address                 // 根据地址(host port)查找该地址对用provider提供的所有服务
 *
 * metrics/registry ... -grep xxx       // 过滤metrics/registry的输出内容
 *
 * quit                                 // 退出
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * jupiter
 * org.jupiter.monitor
 *
 * @author jiachun.fjc
 */
public class MonitorServer extends NettyTcpAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MonitorServer.class);

    private static final int DEFAULT_PORT = 19999;
    private static final AttributeKey<Object> AUTH_KEY = AttributeKey.valueOf("Auth");
    private static final Object AUTH_OBJECT = new Object();
    private static final String DEFAULT_PASSWORD = "e10adc3949ba59abbe56e057f20f883e"; // 123456

    // handlers
    private final CommandHandler handler = new CommandHandler();
    private final StringEncoder encoder = new StringEncoder(UTF8);

    private volatile RegistryMonitor monitor;

    public MonitorServer() {
        this(DEFAULT_PORT);
    }

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

    public void setMonitor(RegistryMonitor monitor) {
        this.monitor = monitor;
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
                if (args == null || args.length == 0) {
                    return;
                }

                Command command = Command.parse(args[0]);
                if (command == null) {
                    ctx.writeAndFlush("invalid command!" + NEWLINE);
                    return;
                }

                switch (command) {
                    case AUTH:
                        if (args.length < 2) {
                            ctx.writeAndFlush("Need password!" + NEWLINE);
                            return;
                        }

                        String password = SystemPropertyUtil.get("monitor.server.password");
                        if (password == null) {
                            password = DEFAULT_PASSWORD;
                        }

                        if (password.equals(MD5Util.getMD5(args[1]))) {
                            ctx.channel().attr(AUTH_KEY).setIfAbsent(AUTH_OBJECT);
                            ctx.writeAndFlush("OK" + NEWLINE);
                        } else {
                            ctx.writeAndFlush("Permission denied!" + NEWLINE).addListener(CLOSE);
                        }

                        break;
                    case HELP:
                        StringBuilder buf = new StringBuilder();
                        buf.append("-- Help ------------------------------------------------------------------------")
                            .append(NEWLINE);
                        for (Command parent : Command.values()) {
                            buf.append(String.format("%1$-32s", parent.name()))
                                    .append(parent.description())
                                    .append(NEWLINE);

                            for (Command.ChildCommand child : parent.children()) {
                                buf.append(String.format("%1$36s", "-"))
                                        .append(child.name())
                                        .append(' ')
                                        .append(child.description())
                                        .append(NEWLINE);
                            }

                            buf.append(NEWLINE);
                        }
                        ctx.writeAndFlush(buf.toString());

                        break;
                    case METRICS:
                        if (checkAuth(ctx.channel())) {
                            if (args.length < 2) {
                                ctx.writeAndFlush("Need second arg!" + NEWLINE);
                                return;
                            }

                            Command.ChildCommand child = command.parseChild(args[1]);
                            if (child != null) {
                                switch (child) {
                                    case REPORT:
                                        ctx.writeAndFlush(MetricsReporter.report());

                                        break;
                                }
                            } else {
                                ctx.writeAndFlush("Wrong args denied!" + NEWLINE);
                            }
                        }

                        break;
                    case REGISTRY:
                        if (checkAuth(ctx.channel())) {
                            if (args.length < 3) {
                                ctx.writeAndFlush("Need more args!" + NEWLINE);
                                return;
                            }
                            if (monitor == null) {
                                return;
                            }

                            Command.ChildCommand child = command.parseChild(args[1]);
                            if (child != null) {
                                switch (child) {
                                    case ADDRESS: {
                                        Command.ChildCommand target = command.parseChild(args[2]);
                                        if (target == null) {
                                            ctx.writeAndFlush("Wrong args denied!" + NEWLINE);
                                            return;
                                        }

                                        List<String> addresses;
                                        switch (target) {
                                            case P:
                                                addresses = monitor.listPublisherHosts();

                                                break;
                                            case S:
                                                addresses = monitor.listSubscriberAddresses();

                                                break;
                                            default:
                                                return;
                                        }
                                        Command.ChildCommand childGrep = null;
                                        if (args.length >= 5) {
                                            childGrep = command.parseChild(args[3]);
                                        }
                                        for (String a : addresses) {
                                            if (childGrep != null && childGrep == Command.ChildCommand.GREP) {
                                                if (a.contains(args[4])) {
                                                    ctx.writeAndFlush(a + NEWLINE);
                                                }
                                            } else {
                                                ctx.writeAndFlush(a + NEWLINE);
                                            }
                                        }

                                        break;
                                    }
                                    case BY_SERVICE: {
                                        if (args.length < 5) {
                                            ctx.writeAndFlush("Args[2]: group, args[3]: version, args[4]: serviceProviderName" + NEWLINE);
                                            return;
                                        }
                                        Command.ChildCommand childGrep = null;
                                        if (args.length >= 7) {
                                            childGrep = command.parseChild(args[5]);
                                        }

                                        for (String a : monitor.listAddressesByService(args[2], args[3], args[4])) {
                                            if (childGrep != null && childGrep == Command.ChildCommand.GREP) {
                                                if (a.contains(args[6])) {
                                                    ctx.writeAndFlush(a + NEWLINE);
                                                }
                                            } else {
                                                ctx.writeAndFlush(a + NEWLINE);
                                            }
                                        }

                                        break;
                                    }
                                    case BY_ADDRESS: {
                                        if (args.length < 4) {
                                            ctx.writeAndFlush("Args[2]: host, args[3]: port" + NEWLINE);
                                            return;
                                        }
                                        Command.ChildCommand childGrep = null;
                                        if (args.length >= 6) {
                                            childGrep = command.parseChild(args[4]);
                                        }

                                        for (String a : monitor.listServicesByAddress(args[2], Integer.parseInt(args[3]))) {
                                            if (childGrep != null && childGrep == Command.ChildCommand.GREP) {
                                                if (a.contains(args[5])) {
                                                    ctx.writeAndFlush(a + NEWLINE);
                                                }
                                            } else {
                                                ctx.writeAndFlush(a + NEWLINE);
                                            }
                                        }

                                        break;
                                    }
                                }
                            } else {
                                ctx.writeAndFlush("Wrong args denied!" + NEWLINE);
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
