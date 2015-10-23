package org.jupiter.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;
import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.rpc.AbstractJClient;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.JOption;
import org.jupiter.transport.netty.channel.NettyChannelGroup;

import java.util.concurrent.ThreadFactory;

import static org.jupiter.common.util.JConstants.AVAILABLE_PROCESSORS;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyConnector extends AbstractJClient implements JConnector<JConnection> {

    protected final Protocol protocol;
    protected final HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("connector.timer"));

    private Bootstrap bootstrap;
    private EventLoopGroup worker;
    private int nWorkers;

    protected volatile ByteBufAllocator allocator;

    public NettyConnector(Protocol protocol) {
        this(protocol, AVAILABLE_PROCESSORS + 1);
    }

    public NettyConnector(Protocol protocol, int nWorkers) {
        this.protocol = protocol;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory workerFactory = new DefaultThreadFactory("jupiter.connector");
        worker = initEventLoopGroup(nWorkers, workerFactory);

        bootstrap = new Bootstrap().group(worker);

        JConfig child = config();
        child.setOption(JOption.IO_RATIO, 100);
        child.setOption(JOption.PREFER_DIRECT, true);
        child.setOption(JOption.USE_POOLED_ALLOCATOR, true);

        doInit();
    }

    protected abstract void doInit();

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public void shutdownGracefully() {
        worker.shutdownGracefully();
    }

    @Override
    protected JChannelGroup newChannelGroup(UnresolvedAddress address) {
        return new NettyChannelGroup(address);
    }

    protected void setOptions() {
        JConfig child = config();

        setIoRatio(child.getOption(JOption.IO_RATIO));

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

    public abstract void setIoRatio(int workerIoRatio);

    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);

    public Bootstrap bootstrap() {
        return bootstrap;
    }

    public EventLoopGroup worker() {
        return worker;
    }
}
