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
import org.jupiter.common.util.internal.UnsafeAccess;
import org.jupiter.registry.NotifyListener;
import org.jupiter.registry.OfflineListener;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.rpc.AbstractJClient;
import org.jupiter.rpc.Directory;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.transport.*;
import org.jupiter.transport.netty.channel.NettyChannelGroup;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notifyCondition = lock.newCondition();
    private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

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
    public ConnectionManagement manageConnections(final Directory directory) {

        subscribe(directory, new NotifyListener() {

            @Override
            public void notify(List<RegisterMeta> registerMetaList) {
                for (RegisterMeta meta : registerMetaList) {
                    UnresolvedAddress address = new UnresolvedAddress(meta.getHost(), meta.getPort());
                    JChannelGroup group = group(address);
                    if (group.isEmpty()) {
                        JConnection connection = connect(address);
                        JConnectionManager.manage(connection);

                        subscribe(address, new OfflineListener() {

                            @Override
                            public void offline(RegisterMeta.Address address) {
                                JConnectionManager.cancelReconnect(UnresolvedAddress.cast(address));
                            }
                        });
                    }

                    addGroup(directory, group);

                    if (signalNeeded.getAndSet(false)) {
                        ReentrantLock _look = lock;
                        _look.lock();
                        try {
                            notifyCondition.signalAll();
                        } finally {
                            _look.unlock();
                        }
                    }
                }
            }
        });

        return new ConnectionManagement() {

            @Override
            public Directory directory() {
                return directory;
            }
        };
    }

    @Override
    public void waitForAvailable(ConnectionManagement management, long timeoutMillis) {
        Directory directory = management.directory();

        if (isDirectoryAvailable(directory)) {
            return;
        }

        long start = System.nanoTime();
        final ReentrantLock _look = lock;
        _look.lock();
        try {
            while (!isDirectoryAvailable(directory)) {
                signalNeeded.getAndSet(true);
                notifyCondition.await(timeoutMillis, MILLISECONDS);

                if (isDirectoryAvailable(directory) || (System.nanoTime() - start) > MILLISECONDS.toNanos(timeoutMillis)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            UnsafeAccess.UNSAFE.throwException(e);
        } finally {
            _look.unlock();
        }
    }

    @Override
    public void shutdownGracefully() {
        worker.shutdownGracefully();
    }

    @Override
    protected JChannelGroup newChannelGroup(UnresolvedAddress address) {
        return new NettyChannelGroup(address);
    }

    public Bootstrap bootstrap() {
        return bootstrap;
    }

    public EventLoopGroup worker() {
        return worker;
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
}
