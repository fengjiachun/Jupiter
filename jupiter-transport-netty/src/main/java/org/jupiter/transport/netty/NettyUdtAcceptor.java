package org.jupiter.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.udt.nio.NioUdtProvider;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.JConfigGroup;

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
public abstract class NettyUdtAcceptor extends NettyAcceptor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyUdtAcceptor.class);

    private final NettyConfig.NettyUDTConfigGroup configGroup = new NettyConfig.NettyUDTConfigGroup();

    public NettyUdtAcceptor(int port) {
        super(Protocol.UDT, new InetSocketAddress(port));
        init();
    }

    public NettyUdtAcceptor(SocketAddress address) {
        super(Protocol.UDT, address);
        init();
    }

    public NettyUdtAcceptor(int port, int nWorks) {
        super(Protocol.UDT, new InetSocketAddress(port), nWorks);
        init();
    }

    public NettyUdtAcceptor(SocketAddress address, int nWorks) {
        super(Protocol.UDT, address, nWorks);
        init();
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        ServerBootstrap boot = bootstrap();

        // parent options
        NettyConfig.NettyUDTConfigGroup.ParentConfig parent = configGroup.parent();
        boot.option(ChannelOption.SO_BACKLOG, parent.getBacklog());

        // child options
        NettyConfig.NettyUDTConfigGroup.ChildConfig child = configGroup.child();
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
    }

    @Override
    public JConfigGroup configGroup() {
        return configGroup;
    }

    @Override
    public void start() throws InterruptedException {
        // Wait until the server socket is bind succeed.
        ChannelFuture future = bind(address).sync();

        logger.info("Jupiter UDT server start, and will wait until the server socket is closed."
                + NEWLINE + "{}.", toString());

        // Wait until the server socket is closed.
        future.channel().closeFuture().sync();
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
        return "Socket address:[" + address + "]" + NEWLINE + bootstrap();
    }
}
