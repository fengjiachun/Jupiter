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
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConfig;

import java.util.concurrent.ThreadFactory;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyTcpConnector extends NettyConnector {

    private final boolean nativeEt; // Use native epoll ET
    private final NettyConfig.NettyTcpConfigGroup.ChildConfig childConfig = new NettyConfig.NettyTcpConfigGroup.ChildConfig();

    public NettyTcpConnector() {
        super(Protocol.TCP);
        nativeEt = true;
        init();
    }

    public NettyTcpConnector(String appName) {
        super(appName, Protocol.TCP);
        nativeEt = true;
        init();
    }

    public NettyTcpConnector(boolean nativeEt) {
        super(Protocol.TCP);
        this.nativeEt = nativeEt;
        init();
    }

    public NettyTcpConnector(String appName, boolean nativeEt) {
        super(appName, Protocol.TCP);
        this.nativeEt = nativeEt;
        init();
    }

    public NettyTcpConnector(int nWorkers) {
        super(Protocol.TCP, nWorkers);
        nativeEt = true;
        init();
    }

    public NettyTcpConnector(String appName, int nWorkers) {
        super(appName, Protocol.TCP, nWorkers);
        nativeEt = true;
        init();
    }

    public NettyTcpConnector(int nWorkers, boolean nativeEt) {
        super(Protocol.TCP, nWorkers);
        this.nativeEt = nativeEt;
        init();
    }

    public NettyTcpConnector(String appName, int nWorkers, boolean nativeEt) {
        super(appName, Protocol.TCP, nWorkers);
        this.nativeEt = nativeEt;
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
    }

    @Override
    public JConnection connect(UnresolvedAddress remoteAddress) {
        return connect(remoteAddress, false);
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
        } else if (worker instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) worker).setIoRatio(workerIoRatio);
        }
    }

    @Override
    protected EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory) {
        return isNativeEt() ? new EpollEventLoopGroup(nThreads, tFactory) : new NioEventLoopGroup(nThreads, tFactory);
    }

    /**
     * Netty provides the native socket transport for Linux using JNI based on Epoll Edge Triggered(ET).
     */
    public boolean isNativeEt() {
        return nativeEt && NativeSupport.isSupportNativeET();
    }
}
