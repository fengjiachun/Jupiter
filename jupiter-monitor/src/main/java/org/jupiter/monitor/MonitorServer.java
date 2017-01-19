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

package org.jupiter.monitor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.ReferenceCountUtil;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.monitor.handler.CommandHandler;
import org.jupiter.monitor.handler.RegistryHandler;
import org.jupiter.registry.RegistryMonitor;
import org.jupiter.transport.netty.NettyTcpAcceptor;
import org.jupiter.transport.netty.TcpChannelProvider;

import java.net.SocketAddress;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * 监控服务, RegistryServer与ProviderServer都应该启用
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
 * registry -by_service                 // 根据服务(group providerServiceName version)查找所有提供当前服务的机器地址列表
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

    // handlers
    private final TelnetHandler handler = new TelnetHandler();
    private final StringEncoder encoder = new StringEncoder(JConstants.UTF8);

    private volatile RegistryMonitor registryMonitor;

    public MonitorServer() {
        this(DEFAULT_PORT);
    }

    public MonitorServer(int port) {
        super(port, 1, false);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        ServerBootstrap boot = bootstrap();

        boot.channelFactory(TcpChannelProvider.NIO_ACCEPTOR)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()),
                                new StringDecoder(JConstants.UTF8),
                                encoder,
                                handler);
                    }
                });

        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    public void start() throws InterruptedException {
        super.start(false);
    }

    /**
     * For jupiter-registry-default
     */
    public void setRegistryMonitor(RegistryMonitor registryMonitor) {
        this.registryMonitor = registryMonitor;
    }

    @ChannelHandler.Sharable
    class TelnetHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof String) {
                String[] args = Strings.split(((String) msg).replace("\r\n", ""), ' ');
                if (args == null || args.length == 0) {
                    return;
                }

                Command command = Command.parse(args[0]);
                if (command == null) {
                    ctx.writeAndFlush("invalid command!" + JConstants.NEWLINE);
                    return;
                }

                CommandHandler handler = command.handler();
                if (handler instanceof RegistryHandler) {
                    if (((RegistryHandler) handler).getRegistryMonitor() != registryMonitor) {
                        ((RegistryHandler) handler).setRegistryMonitor(registryMonitor);
                    }
                }
                handler.handle(ctx.channel(), command, args);
            } else {
                logger.warn("Unexpected msg type received: {}.", msg.getClass());

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
