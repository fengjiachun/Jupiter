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
import io.netty.channel.nio.NioEventLoopGroup;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.JConfigGroup;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyTcpAcceptor extends NettyAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyTcpAcceptor.class);

    private final boolean isNative; // use native transport
    private final NettyConfig.NettyTcpConfigGroup configGroup = new NettyConfig.NettyTcpConfigGroup();

    public NettyTcpAcceptor(int port) {
        super(Protocol.TCP, new InetSocketAddress(port));
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress) {
        super(Protocol.TCP, localAddress);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(int port, int nWorkers) {
        super(Protocol.TCP, new InetSocketAddress(port), nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(int port, int nBosses, int nWorkers) {
        super(Protocol.TCP, new InetSocketAddress(port), nBosses, nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nWorkers) {
        super(Protocol.TCP, localAddress, nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nBosses, int nWorkers) {
        super(Protocol.TCP, localAddress, nBosses, nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(int port, boolean isNative) {
        super(Protocol.TCP, new InetSocketAddress(port));
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, boolean isNative) {
        super(Protocol.TCP, localAddress);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(int port, int nWorkers, boolean isNative) {
        super(Protocol.TCP, new InetSocketAddress(port), nWorkers);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(int port, int nBosses, int nWorkers, boolean isNative) {
        super(Protocol.TCP, new InetSocketAddress(port), nBosses, nWorkers);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nWorkers, boolean isNative) {
        super(Protocol.TCP, localAddress, nWorkers);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nBosses, int nWorkers, boolean isNative) {
        super(Protocol.TCP, localAddress, nBosses, nWorkers);
        this.isNative = isNative;
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
        } else if (boss instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) boss).setIoRatio(bossIoRatio);
        }

        EventLoopGroup worker = worker();
        if (worker instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) worker).setIoRatio(workerIoRatio);
        } else if (worker instanceof KQueueEventLoopGroup) {
            ((KQueueEventLoopGroup) worker).setIoRatio(workerIoRatio);
        } else if (worker instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) worker).setIoRatio(workerIoRatio);
        }
    }

    @Override
    protected EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory) {
        TcpChannelProvider.SocketType socketType = socketType();
        switch (socketType) {
            case NATIVE_EPOLL:
                return new EpollEventLoopGroup(nThreads, tFactory);
            case NATIVE_KQUEUE:
                return new KQueueEventLoopGroup(nThreads, tFactory);
            case JAVA_NIO:
                return new NioEventLoopGroup(nThreads, tFactory);
            default:
                throw new IllegalStateException("invalid socket type: " + socketType);
        }
    }

    protected void initChannelFactory() {
        TcpChannelProvider.SocketType socketType = socketType();
        switch (socketType) {
            case NATIVE_EPOLL:
                bootstrap().channelFactory(TcpChannelProvider.NATIVE_EPOLL_ACCEPTOR);
                break;
            case NATIVE_KQUEUE:
                bootstrap().channelFactory(TcpChannelProvider.NATIVE_KQUEUE_ACCEPTOR);
                break;
            case JAVA_NIO:
                bootstrap().channelFactory(TcpChannelProvider.JAVA_NIO_ACCEPTOR);
                break;
            default:
                throw new IllegalStateException("invalid socket type: " + socketType);
        }
    }

    private TcpChannelProvider.SocketType socketType() {
        if (isNative && NativeSupport.isNativeEPollAvailable()) {
            // netty provides the native socket transport for Linux using JNI.
            return TcpChannelProvider.SocketType.NATIVE_EPOLL;
        }
        if (isNative && NativeSupport.isNativeKQueueAvailable()) {
            // netty provides the native socket transport for BSD systems such as MacOS using JNI.
            return TcpChannelProvider.SocketType.NATIVE_KQUEUE;
        }
        return TcpChannelProvider.SocketType.JAVA_NIO;
    }

    @Override
    public String toString() {
        return "Socket address:[" + localAddress + ']'
                + ", socket type: " + socketType()
                + JConstants.NEWLINE
                + bootstrap();
    }
}
