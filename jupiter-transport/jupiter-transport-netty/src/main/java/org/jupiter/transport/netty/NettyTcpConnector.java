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
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.jupiter.common.util.JConstants;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.UnresolvedAddress;

import java.util.concurrent.ThreadFactory;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyTcpConnector extends NettyConnector {

    private final boolean isNative; // use native transport
    private final NettyConfig.NettyTcpConfigGroup.ChildConfig childConfig = new NettyConfig.NettyTcpConfigGroup.ChildConfig();

    public NettyTcpConnector() {
        super(Protocol.TCP);
        isNative = false;
        init();
    }

    public NettyTcpConnector(boolean isNative) {
        super(Protocol.TCP);
        this.isNative = isNative;
        init();
    }

    public NettyTcpConnector(int nWorkers) {
        super(Protocol.TCP, nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpConnector(int nWorkers, boolean isNative) {
        super(Protocol.TCP, nWorkers);
        this.isNative = isNative;
        init();
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        Bootstrap boot = bootstrap();

        NettyConfig.NettyTcpConfigGroup.ChildConfig child = childConfig;

        // child options
        boot.option(ChannelOption.SO_REUSEADDR, child.isReuseAddress())
                .option(ChannelOption.SO_KEEPALIVE, child.isKeepAlive())
                .option(ChannelOption.TCP_NODELAY, child.isTcpNoDelay())
                .option(ChannelOption.ALLOW_HALF_CLOSURE, child.isAllowHalfClosure());
        if (child.getRcvBuf() > 0) {
            boot.option(ChannelOption.SO_RCVBUF, child.getRcvBuf());
        }
        if (child.getSndBuf() > 0) {
            boot.option(ChannelOption.SO_SNDBUF, child.getSndBuf());
        }
        if (child.getLinger() > 0) {
            boot.option(ChannelOption.SO_LINGER, child.getLinger());
        }
        if (child.getIpTos() > 0) {
            boot.option(ChannelOption.IP_TOS, child.getIpTos());
        }
        if (child.getConnectTimeoutMillis() > 0) {
            boot.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, child.getConnectTimeoutMillis());
        }
        int bufLowWaterMark = child.getWriteBufferLowWaterMark();
        int bufHighWaterMark = child.getWriteBufferHighWaterMark();
        WriteBufferWaterMark waterMark;
        if (bufLowWaterMark >= 0 && bufHighWaterMark > 0) {
            waterMark = new WriteBufferWaterMark(bufLowWaterMark, bufHighWaterMark);
        } else {
            waterMark = new WriteBufferWaterMark(512 * 1024, 1024 * 1024);
        }
        boot.option(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
    }

    @Override
    public JConnection connect(UnresolvedAddress address) {
        return connect(address, false);
    }

    @Override
    public JConfig config() {
        return childConfig;
    }

    @Override
    public void setIoRatio(int workerIoRatio) {
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
                throw new IllegalStateException("Invalid socket type: " + socketType);
        }
    }

    protected void initChannelFactory() {
        TcpChannelProvider.SocketType socketType = socketType();
        switch (socketType) {
            case NATIVE_EPOLL:
                bootstrap().channelFactory(TcpChannelProvider.NATIVE_EPOLL_CONNECTOR);
                break;
            case NATIVE_KQUEUE:
                bootstrap().channelFactory(TcpChannelProvider.NATIVE_KQUEUE_CONNECTOR);
                break;
            case JAVA_NIO:
                bootstrap().channelFactory(TcpChannelProvider.JAVA_NIO_CONNECTOR);
                break;
            default:
                throw new IllegalStateException("Invalid socket type: " + socketType);
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
        return "Socket type: " + socketType()
                + JConstants.NEWLINE
                + bootstrap();
    }
}
