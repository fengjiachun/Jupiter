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

package org.jupiter.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.consumer.processor.DefaultConsumerProcessor;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JOption;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.ProtocolDecoder;
import org.jupiter.transport.netty.handler.ProtocolEncoder;
import org.jupiter.transport.netty.handler.connector.ConnectionWatchdog;
import org.jupiter.transport.netty.handler.connector.ConnectorHandler;
import org.jupiter.transport.netty.handler.connector.ConnectorIdleStateTrigger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jupiter.common.util.JConstants.WRITER_IDLE_TIME_SECONDS;

/**
 * Jupiter tcp connector based on netty.
 *
 *************************************************************************
 *                      ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *
 *                 ─ ─ ─│        Server         │─ ─▷
 *                 │                                 │
 *                      └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *                 │                                 ▽
 *                                              I/O Response
 *                 │                                 │
 *
 *                 │                                 │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 * │               │                                 │                │
 *
 * │               │                                 │                │
 *   ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐      ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ─
 * │  ConnectionWatchdog#outbound        ConnectionWatchdog#inbound│  │
 *   └ ─ ─ ─ ─ ─ ─ △ ─ ─ ─ ─ ─ ─ ┘      └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 * │                                                 │                │
 *                 │
 * │  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐   │
 *     IdleStateChecker#outBound         IdleStateChecker#inBound
 * │  └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘   │
 *                                                   │
 * │               │                                                  │
 *                                      ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐
 * │               │                     ConnectorIdleStateTrigger    │
 *                                      └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 * │               │                                 │                │
 *
 * │               │                    ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐   │
 *                                            ProtocolDecoder
 * │               │                    └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘   │
 *                                                   │
 * │               │                                                  │
 *                                      ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐
 * │               │                         ConnectorHandler         │
 *    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐       └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 * │        ProtocolEncoder                          │                │
 *    └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘
 * │                                                 │                │
 * ─ ─ ─ ─ ─ ─ ─ ─ ┼ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 *                                       ┌ ─ ─ ─ ─ ─ ▽ ─ ─ ─ ─ ─ ┐
 *                 │
 *                                       │       Processor       │
 *                 │
 *            I/O Request                └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *
 *
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class JNettyTcpConnector extends NettyTcpConnector {

    // handlers
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final ConnectorHandler handler = new ConnectorHandler(new DefaultConsumerProcessor());
    private final ProtocolEncoder encoder = new ProtocolEncoder();

    public JNettyTcpConnector() {}

    public JNettyTcpConnector(String appName) {
        super(appName);
    }

    public JNettyTcpConnector(boolean nativeEt) {
        super(nativeEt);
    }

    public JNettyTcpConnector(String appName, boolean nativeEt) {
        super(appName, nativeEt);
    }

    public JNettyTcpConnector(int nWorkers) {
        super(nWorkers);
    }

    public JNettyTcpConnector(String appName, int nWorkers) {
        super(appName, nWorkers);
    }

    public JNettyTcpConnector(int nWorkers, boolean nativeEt) {
        super(nWorkers, nativeEt);
    }

    public JNettyTcpConnector(String appName, int nWorkers, boolean nativeEt) {
        super(appName, nWorkers, nativeEt);
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) SECONDS.toMillis(3));
        // channel factory
        if (isNativeEt()) {
            bootstrap().channel(EpollSocketChannel.class);
        } else {
            bootstrap().channel(NioSocketChannel.class);
        }
    }

    @Override
    public JConnection connect(UnresolvedAddress address, boolean async) {
        setOptions();

        final Bootstrap boot = bootstrap();
        final SocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());
        final JChannelGroup group = group(address);

        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, socketAddress, group) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new IdleStateChecker(timer, 0, WRITER_IDLE_TIME_SECONDS, 0),
                        idleStateTrigger,
                        new ProtocolDecoder(),
                        encoder,
                        handler
                };
            }
        };
        watchdog.setReconnect(true);

        ChannelFuture future;
        try {
            synchronized (bootstrapLock()) {
                boot.handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect(socketAddress);
            }

            // 以下代码在synchronized同步块外面是安全的
            if (!async) {
                future.sync();
            }
        } catch (Throwable t) {
            throw new ConnectFailedException("connects to [" + address + "] fails", t);
        }

        return new JNettyConnection(address, future) {

            @Override
            public void setReconnect(boolean reconnect) {
                watchdog.setReconnect(reconnect);
            }
        };
    }
}
