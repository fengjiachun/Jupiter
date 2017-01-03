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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.nio.NioUdtProvider;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.JConfigGroup;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import static org.jupiter.common.util.JConstants.NEWLINE;
import static org.jupiter.transport.netty.NettyConfig.*;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyUdtAcceptor extends NettyAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyUdtAcceptor.class);

    private final NettyUdtConfigGroup configGroup = new NettyUdtConfigGroup();

    public NettyUdtAcceptor(int port) {
        super(Protocol.UDT, new InetSocketAddress(port));
        init();
    }

    public NettyUdtAcceptor(SocketAddress localAddress) {
        super(Protocol.UDT, localAddress);
        init();
    }

    public NettyUdtAcceptor(int port, int nWorks) {
        super(Protocol.UDT, new InetSocketAddress(port), nWorks);
        init();
    }

    public NettyUdtAcceptor(SocketAddress localAddress, int nWorks) {
        super(Protocol.UDT, localAddress, nWorks);
        init();
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        ServerBootstrap boot = bootstrap();

        // parent options
        NettyUdtConfigGroup.ParentConfig parent = configGroup.parent();
        boot.option(ChannelOption.SO_BACKLOG, parent.getBacklog());

        // child options
        NettyUdtConfigGroup.ChildConfig child = configGroup.child();
        boot.childOption(ChannelOption.SO_REUSEADDR, child.isReuseAddress());
        if (child.getRcvBuf() > 0) {
            boot.childOption(ChannelOption.SO_RCVBUF, child.getRcvBuf());
        }
        if (child.getSndBuf() > 0) {
            boot.childOption(ChannelOption.SO_SNDBUF, child.getSndBuf());
        }
        if (child.getLinger() > 0) {
            boot.childOption(ChannelOption.SO_LINGER, child.getLinger());
        }
        int bufLowWaterMark = child.getWriteBufferLowWaterMark();
        int bufHighWaterMark = child.getWriteBufferHighWaterMark();
        if (bufLowWaterMark >= 0 && bufHighWaterMark > 0) {
            WriteBufferWaterMark waterMark = new WriteBufferWaterMark(bufLowWaterMark, bufHighWaterMark);
            boot.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
        }
    }

    @Override
    public JConfigGroup configGroup() {
        return configGroup;
    }

    @Override
    public void start() throws InterruptedException {
        start(true);
    }

    @Override
    public void start(boolean sync) throws InterruptedException {
        // wait until the server socket is bind succeed.
        ChannelFuture future = bind(localAddress).sync();

        logger.info("Jupiter UDT server start" + (sync ? ", and waits until the server socket closed." : ".")
                + NEWLINE + " {}.", toString());

        if (sync) {
            // wait until the server socket is closed.
            future.channel().closeFuture().sync();
        }
    }

    @Override
    public void setIoRatio(int bossIoRatio, int workerIoRatio) {
        ((NioEventLoopGroup) boss()).setIoRatio(bossIoRatio);
        ((NioEventLoopGroup) worker()).setIoRatio(workerIoRatio);
    }

    @Override
    protected EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory) {
        return new NioEventLoopGroup(nThreads, tFactory, NioUdtProvider.BYTE_PROVIDER);
    }

    @Override
    public String toString() {
        return "Socket localAddress:[" + localAddress + "]" + NEWLINE + bootstrap();
    }
}
