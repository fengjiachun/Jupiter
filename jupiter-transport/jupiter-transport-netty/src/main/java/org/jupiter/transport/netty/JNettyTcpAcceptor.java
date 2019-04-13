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

import java.net.SocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;

import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Requires;
import org.jupiter.transport.CodecConfig;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JOption;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.LowCopyProtocolDecoder;
import org.jupiter.transport.netty.handler.LowCopyProtocolEncoder;
import org.jupiter.transport.netty.handler.ProtocolDecoder;
import org.jupiter.transport.netty.handler.ProtocolEncoder;
import org.jupiter.transport.netty.handler.acceptor.AcceptorHandler;
import org.jupiter.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;
import org.jupiter.transport.processor.ProviderProcessor;

/**
 * Jupiter tcp acceptor based on netty.
 *
 * <pre>
 * *********************************************************************
 *            I/O Request                       I/O Response
 *                 │                                 △
 *                                                   │
 *                 │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┼ ─ ─ ─ ─ ─ ─ ─ ─
 * │               │                                                  │
 *                                                   │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
 *     IdleStateChecker#inBound          IdleStateChecker#outBound
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘   │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐                                     │
 *     AcceptorIdleStateTrigger                      │
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                                     │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
 *          ProtocolDecoder                   ProtocolEncoder
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘   │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐                                     │
 *          AcceptorHandler                          │
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                                     │
 *                 │                                 │
 * │                    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐                     │
 *                 ▽                                 │
 * │               ─ ─ ▷│       Processor       ├ ─ ─▷                │
 *
 * │                    └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                     │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 * </pre>
 *
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class JNettyTcpAcceptor extends NettyTcpAcceptor {

    public static final int DEFAULT_ACCEPTOR_PORT = 18090;

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final ChannelOutboundHandler encoder =
            CodecConfig.isCodecLowCopy() ? new LowCopyProtocolEncoder() : new ProtocolEncoder();
    private final AcceptorHandler handler = new AcceptorHandler();

    public JNettyTcpAcceptor() {
        super(DEFAULT_ACCEPTOR_PORT);
    }

    public JNettyTcpAcceptor(int port) {
        super(port);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress) {
        super(localAddress);
    }

    public JNettyTcpAcceptor(int port, int nWorkers) {
        super(port, nWorkers);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress, int nWorkers) {
        super(localAddress, nWorkers);
    }

    public JNettyTcpAcceptor(int port, boolean isNative) {
        super(port, isNative);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress, boolean isNative) {
        super(localAddress, isNative);
    }

    public JNettyTcpAcceptor(int port, int nWorkers, boolean isNative) {
        super(port, nWorkers, isNative);
    }

    public JNettyTcpAcceptor(SocketAddress localAddress, int nWorkers, boolean isNative) {
        super(localAddress, nWorkers, isNative);
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
                        CodecConfig.isCodecLowCopy() ? new LowCopyProtocolDecoder() : new ProtocolDecoder(),
                        encoder,
                        handler);
            }
        });

        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    protected void setProcessor(ProviderProcessor processor) {
        handler.processor(Requires.requireNotNull(processor, "processor"));
    }
}
