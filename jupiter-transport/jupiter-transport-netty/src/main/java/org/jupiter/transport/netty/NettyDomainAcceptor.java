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
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.JConfigGroup;

import java.util.concurrent.ThreadFactory;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyDomainAcceptor extends NettyAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyDomainAcceptor.class);

    private final NettyConfig.NettyTcpConfigGroup configGroup = new NettyConfig.NettyTcpConfigGroup();

    public NettyDomainAcceptor(DomainSocketAddress domainAddress) {
        super(Protocol.DOMAIN, domainAddress);
        init();
    }

    public NettyDomainAcceptor(DomainSocketAddress domainAddress, int nWorkers) {
        super(Protocol.DOMAIN, domainAddress, nWorkers);
        init();
    }

    public NettyDomainAcceptor(DomainSocketAddress domainAddress, int nBosses, int nWorkers) {
        super(Protocol.DOMAIN, domainAddress, nBosses, nWorkers);
        init();
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        ServerBootstrap boot = bootstrap();

        // parent options
        NettyConfig.NettyTcpConfigGroup.ParentConfig parent = configGroup.parent();
        boot.option(ChannelOption.SO_BACKLOG, parent.getBacklog());
        boot.option(ChannelOption.SO_REUSEADDR, parent.isReuseAddress());
        if (parent.getRcvBuf() > 0) {
            boot.option(ChannelOption.SO_RCVBUF, parent.getRcvBuf());
        }

        // child options
        NettyConfig.NettyTcpConfigGroup.ChildConfig child = configGroup.child();
        boot.childOption(ChannelOption.SO_REUSEADDR, child.isReuseAddress())
                .childOption(ChannelOption.SO_KEEPALIVE, child.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, child.isTcpNoDelay())
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, child.isAllowHalfClosure());
        if (child.getRcvBuf() > 0) {
            boot.childOption(ChannelOption.SO_RCVBUF, child.getRcvBuf());
        }
        if (child.getSndBuf() > 0) {
            boot.childOption(ChannelOption.SO_SNDBUF, child.getSndBuf());
        }
        if (child.getLinger() > 0) {
            boot.childOption(ChannelOption.SO_LINGER, child.getLinger());
        }
        if (child.getIpTos() > 0) {
            boot.childOption(ChannelOption.IP_TOS, child.getIpTos());
        }
        int bufLowWaterMark = child.getWriteBufferLowWaterMark();
        int bufHighWaterMark = child.getWriteBufferHighWaterMark();
        WriteBufferWaterMark waterMark;
        if (bufLowWaterMark >= 0 && bufHighWaterMark > 0) {
            waterMark = new WriteBufferWaterMark(bufLowWaterMark, bufHighWaterMark);
        } else {
            waterMark = new WriteBufferWaterMark(512 * 1024, 1024 * 1024);
        }
        boot.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
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

        if (logger.isInfoEnabled()) {
            logger.info("Jupiter TCP server start" + (sync ? ", and waits until the server socket closed." : ".")
                    + JConstants.NEWLINE + " {}.", toString());
        }

        if (sync) {
            // wait until the server socket is closed.
            future.channel().closeFuture().sync();
        }
    }

    @Override
    public void setIoRatio(int bossIoRatio, int workerIoRatio) {
        EventLoopGroup boss = boss();
        if (boss instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) boss).setIoRatio(bossIoRatio);
        } else if (boss instanceof KQueueEventLoopGroup) {
            ((KQueueEventLoopGroup) boss).setIoRatio(bossIoRatio);
        }

        EventLoopGroup worker = worker();
        if (worker instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) worker).setIoRatio(workerIoRatio);
        } else if (worker instanceof KQueueEventLoopGroup) {
            ((KQueueEventLoopGroup) worker).setIoRatio(workerIoRatio);
        }
    }

    @Override
    protected EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory) {
        SocketChannelProvider.SocketType socketType = socketType();
        switch (socketType) {
            case NATIVE_EPOLL_DOMAIN:
                return new EpollEventLoopGroup(nThreads, tFactory);
            case NATIVE_KQUEUE_DOMAIN:
                return new KQueueEventLoopGroup(nThreads, tFactory);
            default:
                throw new IllegalStateException("Invalid socket type: " + socketType);
        }
    }

    protected void initChannelFactory() {
        SocketChannelProvider.SocketType socketType = socketType();
        switch (socketType) {
            case NATIVE_EPOLL_DOMAIN:
                bootstrap().channelFactory(SocketChannelProvider.NATIVE_EPOLL_DOMAIN_ACCEPTOR);
                break;
            case NATIVE_KQUEUE_DOMAIN:
                bootstrap().channelFactory(SocketChannelProvider.NATIVE_KQUEUE_DOMAIN_ACCEPTOR);
                break;
            default:
                throw new IllegalStateException("Invalid socket type: " + socketType);
        }
    }

    protected SocketChannelProvider.SocketType socketType() {
        if (NativeSupport.isNativeEPollAvailable()) {
            // netty provides the unix domain  socket transport for Linux using JNI.
            return SocketChannelProvider.SocketType.NATIVE_EPOLL_DOMAIN;
        }
        if (NativeSupport.isNativeKQueueAvailable()) {
            // netty provides the unix domain  socket transport for BSD systems such as MacOS using JNI.
            return SocketChannelProvider.SocketType.NATIVE_KQUEUE_DOMAIN;
        }
        throw new UnsupportedOperationException("Unsupported unix domain socket");
    }

    @Override
    public String toString() {
        return "Socket address:[" + localAddress + ']'
                + ", socket type: " + socketType()
                + JConstants.NEWLINE
                + bootstrap();
    }
}
