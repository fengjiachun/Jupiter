package org.jupiter.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JConfigGroup;
import org.jupiter.transport.JOption;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import static org.jupiter.common.util.JConstants.NEWLINE;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyTcpAcceptor extends NettyAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyTcpAcceptor.class);

    private static final boolean SUPPORT_NATIVE_ET = NativeSupport.supportNativeET();

    private final boolean nativeEt; // Use native epoll ET
    private final NettyConfig.NettyTCPConfigGroup configGroup = new NettyConfig.NettyTCPConfigGroup();

    public NettyTcpAcceptor(int port) {
        super(Protocol.TCP, new InetSocketAddress(port));
        nativeEt = true;
        init();
    }

    public NettyTcpAcceptor(SocketAddress address) {
        super(Protocol.TCP, address);
        nativeEt = true;
        init();
    }

    public NettyTcpAcceptor(int port, int nWorks) {
        super(Protocol.TCP, new InetSocketAddress(port), nWorks);
        nativeEt = true;
        init();
    }

    public NettyTcpAcceptor(SocketAddress address, int nWorks) {
        super(Protocol.TCP, address, nWorks);
        nativeEt = true;
        init();
    }

    public NettyTcpAcceptor(int port, boolean nativeEt) {
        super(Protocol.TCP, new InetSocketAddress(port));
        this.nativeEt = nativeEt;
        init();
    }

    public NettyTcpAcceptor(SocketAddress address, boolean nativeEt) {
        super(Protocol.TCP, address);
        this.nativeEt = nativeEt;
        init();
    }

    public NettyTcpAcceptor(int port, int nWorks, boolean nativeEt) {
        super(Protocol.TCP, new InetSocketAddress(port), nWorks);
        this.nativeEt = nativeEt;
        init();
    }

    public NettyTcpAcceptor(SocketAddress address, int nWorks, boolean nativeEt) {
        super(Protocol.TCP, address, nWorks);
        this.nativeEt = nativeEt;
        init();
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup.parent();
        parent.setOption(JOption.SO_BACKLOG, 32768);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup.child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        ServerBootstrap boot = bootstrap();

        // parent options
        NettyConfig.NettyTCPConfigGroup.ParentConfig parent = configGroup.parent();
        boot.option(ChannelOption.SO_BACKLOG, parent.getBacklog());
        boot.option(ChannelOption.SO_REUSEADDR, parent.isReuseAddress());
        if (parent.getRcvBuf() > 0) {
            boot.option(ChannelOption.SO_RCVBUF, parent.getRcvBuf());
        }

        // child options
        NettyConfig.NettyTCPConfigGroup.ChildConfig child = configGroup.child();
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
    }

    @Override
    public JConfigGroup configGroup() {
        return configGroup;
    }

    @Override
    public void start() throws InterruptedException {
        // Wait until the server socket is bind succeed.
        ChannelFuture future = bind(address).sync();

        logger.info("Jupiter TCP server start, and will wait until the server socket is closed."
                + NEWLINE + "{}.", toString());

        // Wait until the server socket is closed.
        future.channel().closeFuture().sync();
    }

    @Override
    public void setIoRatio(int bossIoRatio, int workerIoRatio) {
        EventLoopGroup boss = boss();
        if (boss instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) boss).setIoRatio(bossIoRatio);
        } else if (boss instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) boss).setIoRatio(bossIoRatio);
        }

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

    public boolean isNativeEt() {
        return nativeEt && SUPPORT_NATIVE_ET;
    }

    @Override
    public String toString() {
        return "Socket address:[" + address + ']' + ", nativeET: " + isNativeEt()
                + NEWLINE + bootstrap();
    }
}
