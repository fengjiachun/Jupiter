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

package org.jupiter.transport.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.SystemClock;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.JConstants.DEFAULT_WARM_UP;
import static org.jupiter.common.util.JConstants.DEFAULT_WEIGHT;
import static org.jupiter.common.util.internal.UnsafeAccess.UNSAFE;

/**
 * jupiter
 * org.jupiter.transport.netty.channel
 *
 * @author jiachun.fjc
 */
public class NettyChannelGroup implements JChannelGroup {

    private static final long ELEMENTS_OFFSET;
    static {
        long offset;
        try {
            Field field = Reflects.getField(CopyOnWriteArrayList.class, "array");
            offset = UNSAFE.objectFieldOffset(field);
        } catch (Exception e) {
            offset = 0;
        }
        ELEMENTS_OFFSET = offset;
    }

    private final CopyOnWriteArrayList<NettyChannel> channels = new CopyOnWriteArrayList<>();

    // 连接断开时自动被移除
    private final ChannelFutureListener remover = new ChannelFutureListener() {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            remove(NettyChannel.attachChannel(future.channel()));
        }
    };

    private final AtomicInteger index = new AtomicInteger();
    private final UnresolvedAddress address;

    private volatile int weight = DEFAULT_WEIGHT; // the weight if this group
    private volatile int warmUp = DEFAULT_WARM_UP; // warm-up time
    private volatile long timestamp = SystemClock.millisClock().now();
    private volatile long lossTimestamp = -1;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notifyCondition = lock.newCondition();
    // attempts to elide conditional wake-ups when the lock is uncontended.
    private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

    public NettyChannelGroup(UnresolvedAddress address) {
        this.address = address;
    }

    @Override
    public UnresolvedAddress remoteAddress() {
        return address;
    }

    @Override
    public JChannel next() {
        for (;;) {
            // 请原谅下面这段放荡不羁的糟糕代码
            Object[] array; // the snapshot of channels array
            if (ELEMENTS_OFFSET > 0) {
                array = (Object[]) UNSAFE.getObjectVolatile(channels, ELEMENTS_OFFSET);
            } else {
                array = (Object[]) Reflects.getValue(channels, "array");
            }

            if (array.length == 0) {
                if (waitForAvailable(1500)) { // wait a moment
                    continue;
                }
                throw new IllegalStateException("no channel");
            }

            if (array.length == 1) {
                return (JChannel) array[0];
            }

            int offset = Math.abs(index.getAndIncrement() % array.length);

            return (JChannel) array[offset];
        }
    }

    @Override
    public List<? extends JChannel> channels() {
        return Lists.newArrayList(channels);
    }

    @Override
    public boolean isEmpty() {
        return channels.isEmpty();
    }

    @Override
    public boolean add(JChannel channel) {
        boolean added = channel instanceof NettyChannel && channels.add((NettyChannel) channel);
        if (added) {
            ((NettyChannel) channel).channel().closeFuture().addListener(remover);
            lossTimestamp = -1;

            if (signalNeeded.getAndSet(false)) {
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    notifyCondition.signalAll();
                } finally {
                    _look.unlock();
                }
            }
        }
        return added;
    }

    @Override
    public boolean remove(JChannel channel) {
        boolean removed = channel instanceof NettyChannel && channels.remove(channel);
        if (removed && channels.isEmpty()) {
            lossTimestamp = SystemClock.millisClock().now();
        }
        return removed;
    }

    @Override
    public int size() {
        return channels.size();
    }

    @Override
    public boolean isAvailable() {
        return !channels.isEmpty();
    }

    @Override
    public boolean waitForAvailable(long timeoutMillis) {
        if (isAvailable()) {
            return true;
        }

        boolean available = false;
        long start = System.nanoTime();
        final ReentrantLock _look = lock;
        _look.lock();
        try {
            while (!isAvailable()) {
                signalNeeded.getAndSet(true);
                notifyCondition.await(timeoutMillis, MILLISECONDS);

                available = isAvailable();

                if (available || (System.nanoTime() - start) > MILLISECONDS.toNanos(timeoutMillis)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            UNSAFE.throwException(e);
        } finally {
            _look.unlock();
        }

        return available;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public int getWarmUp() {
        return warmUp;
    }

    @Override
    public void setWarmUp(int warmUp) {
        this.warmUp = warmUp;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void resetTimestamp() {
        timestamp = SystemClock.millisClock().now();
    }

    @Override
    public long getLossTimestamp() {
        return lossTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NettyChannelGroup that = (NettyChannelGroup) o;

        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

        return "NettyChannelGroup{" +
                "channels=" + channels +
                ", weight=" + weight +
                ", warmUp=" + warmUp +
                ", time=" + formatter.format(new Date(timestamp)) +
                ", address=" + address +
                '}';
    }
}
