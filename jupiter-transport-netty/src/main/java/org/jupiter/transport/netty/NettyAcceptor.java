package org.jupiter.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.PlatformDependent;
import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.rpc.AbstractJServer;
import org.jupiter.transport.JAcceptor;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JOption;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import static org.jupiter.common.util.JConstants.AVAILABLE_PROCESSORS;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyAcceptor extends AbstractJServer implements JAcceptor<ChannelFuture, Future<?>[]> {

    protected final Protocol protocol;
    protected final SocketAddress address;

    protected final HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("acceptor.timer"));

    private ServerBootstrap bootstrap;
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private int nWorkers;

    protected volatile ByteBufAllocator allocator;

    public NettyAcceptor(Protocol protocol, SocketAddress address) {
        this(protocol, address, AVAILABLE_PROCESSORS + 1);
    }

    public NettyAcceptor(Protocol protocol, SocketAddress address, int nWorkers) {
        this.protocol = protocol;
        this.address = address;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory bossFactory = new DefaultThreadFactory("jupiter.acceptor.boss");
        ThreadFactory workerFactory = new DefaultThreadFactory("jupiter.acceptor.worker");
        boss = initEventLoopGroup(1, bossFactory);
        worker = initEventLoopGroup(nWorkers, workerFactory);

        bootstrap = new ServerBootstrap().group(boss, worker);

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.IO_RATIO, 100);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.IO_RATIO, 100);
        child.setOption(JOption.PREFER_DIRECT, true);
        child.setOption(JOption.USE_POOLED_ALLOCATOR, true);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public SocketAddress localAddress() {
        return address;
    }

    @Override
    public Future<?>[] shutdownGracefully() {
        return new Future<?>[] { boss.shutdownGracefully(), worker.shutdownGracefully() };
    }

    protected void setOptions() {
        JConfig parent = configGroup().parent(); // parent options
        JConfig child = configGroup().child(); // child options

        setIoRatio(parent.getOption(JOption.IO_RATIO), child.getOption(JOption.IO_RATIO));

        boolean direct = child.getOption(JOption.PREFER_DIRECT);
        if (child.getOption(JOption.USE_POOLED_ALLOCATOR)) {
            if (direct) {
                allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new PooledByteBufAllocator(false);
            }
        } else {
            if (direct) {
                allocator = new UnpooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new UnpooledByteBufAllocator(false);
            }
        }
    }

    protected ServerBootstrap bootstrap() {
        return bootstrap;
    }

    protected EventLoopGroup boss() {
        return boss;
    }

    protected EventLoopGroup worker() {
        return worker;
    }

    public abstract void setIoRatio(int bossIoRatio, int workerIoRatio);

    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);
}
