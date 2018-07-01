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
public abstract class NettyDomainConnector extends NettyConnector {

    private final NettyConfig.NettyDomainConfigGroup.ChildConfig childConfig = new NettyConfig.NettyDomainConfigGroup.ChildConfig();

    public NettyDomainConnector() {
        super(Protocol.DOMAIN);
        init();
    }

    public NettyDomainConnector(int nWorkers) {
        super(Protocol.DOMAIN, nWorkers);
        init();
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        Bootstrap boot = bootstrap();

        // child options
        NettyConfig.NettyDomainConfigGroup.ChildConfig child = childConfig;

        WriteBufferWaterMark waterMark =
                createWriteBufferWaterMark(child.getWriteBufferLowWaterMark(), child.getWriteBufferHighWaterMark());

        boot.option(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);

        if (child.getConnectTimeoutMillis() > 0) {
            boot.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, child.getConnectTimeoutMillis());
        }
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
                bootstrap().channelFactory(SocketChannelProvider.NATIVE_EPOLL_DOMAIN_CONNECTOR);
                break;
            case NATIVE_KQUEUE_DOMAIN:
                bootstrap().channelFactory(SocketChannelProvider.NATIVE_KQUEUE_DOMAIN_CONNECTOR);
                break;
            default:
                throw new IllegalStateException("Invalid socket type: " + socketType);
        }
    }

    protected SocketChannelProvider.SocketType socketType() {
        if (NativeSupport.isNativeEPollAvailable()) {
            // netty provides the unix domain socket transport for Linux using JNI.
            return SocketChannelProvider.SocketType.NATIVE_EPOLL_DOMAIN;
        }
        if (NativeSupport.isNativeKQueueAvailable()) {
            // netty provides the unix domain socket transport for BSD systems such as MacOS using JNI.
            return SocketChannelProvider.SocketType.NATIVE_KQUEUE_DOMAIN;
        }
        throw new UnsupportedOperationException("Unsupported unix domain socket");
    }

    @Override
    public String toString() {
        return "Socket type: " + socketType()
                + JConstants.NEWLINE
                + bootstrap();
    }
}
