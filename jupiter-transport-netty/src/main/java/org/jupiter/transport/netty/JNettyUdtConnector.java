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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.consumer.processor.DefaultConsumerProcessor;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JOption;
import org.jupiter.transport.error.ConnectFailedException;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.ProtocolDecoder;
import org.jupiter.transport.netty.handler.ProtocolEncoder;
import org.jupiter.transport.netty.handler.connector.ConnectionWatchdog;
import org.jupiter.transport.netty.handler.connector.ConnectorHandler;
import org.jupiter.transport.netty.handler.connector.ConnectorIdleStateTrigger;

import java.util.concurrent.TimeUnit;

import static org.jupiter.common.util.JConstants.WRITER_IDLE_TIME_SECONDS;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class JNettyUdtConnector extends NettyUdtConnector {

    // handlers
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final ConnectorHandler handler = new ConnectorHandler(new DefaultConsumerProcessor());
    private final ProtocolEncoder encoder = new ProtocolEncoder();

    public JNettyUdtConnector() {}

    public JNettyUdtConnector(int nWorkers) {
        super(nWorkers);
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(3));
        // channel factory
        bootstrap().channelFactory(NioUdtProvider.BYTE_CONNECTOR);
    }

    @Override
    public JConnection connect(UnresolvedAddress remoteAddress, boolean async) {
        setOptions();

        Bootstrap boot = bootstrap();

        JChannelGroup group = group(remoteAddress);

        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, remoteAddress, group) {

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
            }};
        watchdog.setReconnect(true);

        try {
            ChannelFuture future;
            synchronized (bootstrapLock()) {
                boot.handler(new ChannelInitializer<UdtChannel>() {

                    @Override
                    protected void initChannel(UdtChannel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect(remoteAddress.getHost(), remoteAddress.getPort());
            }

            // 以下代码在synchronized同步块外面是安全的
            if (!async) {
                future.sync();
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
}
