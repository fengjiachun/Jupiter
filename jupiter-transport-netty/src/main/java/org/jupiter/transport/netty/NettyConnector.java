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
import org.jupiter.rpc.channel.JChannel;
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
    public ConnectionManager manageConnections(final Directory directory) {

        ConnectionManager manager = new ConnectionManager() {

            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notifyCondition = lock.newCondition();
            private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

            @Override
            public void start() {
                subscribe(directory, new NotifyListener() {

                    @Override
                    public void notify(List<RegisterMeta> registerMetaList) {
                        for (RegisterMeta meta : registerMetaList) {
                            final UnresolvedAddress address = new UnresolvedAddress(meta.getHost(), meta.getPort());
                            final JChannelGroup group = group(address);

                            // 每个group存放的是相同对端地址的channel, 如果group不为空且至少有一个channel自动重连
                            // 被设置为true就不用再建立连接了
                            boolean shouldConnect = true;
                            for (JChannel channel : group.channels()) {
                                if (channel.isActive() && channel.isReconnect()) {
                                    shouldConnect = false;
                                    break;
                                }
                            }
                            if (shouldConnect) {
                                JConnection connection = connect(address);
                                JConnectionManager.manage(connection);

                                offlineListening(address, new OfflineListener() {

                                    @Override
                                    public void offline() {
                                        // 取消自动重连
                                        JConnectionManager.cancelReconnect(address);
                                        // 移除ChannelGroup避免被LoadBalance选中
                                        removeChannelGroup(directory, group);
                                    }
                                });
                            }

                            // 添加ChannelGroup到指定directory
                            addChannelGroup(directory, group);
                        }

                        if (!registerMetaList.isEmpty() && signalNeeded.getAndSet(false)) {
                            ReentrantLock _look = lock;
                            _look.lock();
                            try {
                                notifyCondition.signalAll();
                            } finally {
                                _look.unlock();
                            }
                        }
                    }
                });
            }

            @Override
            public boolean waitForAvailable(long timeoutMillis) {
                if (isDirectoryAvailable(directory)) {
                    return true;
                }

                boolean isAvailable = false;

                long start = System.nanoTime();
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    while (!isDirectoryAvailable(directory)) {
                        signalNeeded.getAndSet(true);
                        notifyCondition.await(timeoutMillis, MILLISECONDS);

                        if (isDirectoryAvailable(directory)) {
                            isAvailable = true;
                            break;
                        }
                        if ((System.nanoTime() - start) > MILLISECONDS.toNanos(timeoutMillis)) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    UnsafeAccess.UNSAFE.throwException(e);
                } finally {
                    _look.unlock();
                }
                return isAvailable;
            }
        };

        manager.start();

        return manager;
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

    public Object bootstrapLock() {
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
