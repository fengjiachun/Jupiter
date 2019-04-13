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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.channel.ChannelFutureListener;

import org.jupiter.common.atomic.AtomicUpdater;
import org.jupiter.common.util.IntSequence;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Requires;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.ThrowUtil;
import org.jupiter.transport.Directory;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JChannelGroup;

/**
 * jupiter
 * org.jupiter.transport.netty.channel
 *
 * @author jiachun.fjc
 */
public class NettyChannelGroup implements JChannelGroup {

    private static long LOSS_INTERVAL = SystemPropertyUtil
            .getLong("jupiter.io.channel.group.loss.interval.millis", TimeUnit.MINUTES.toMillis(5));

    private static int DEFAULT_SEQUENCE_STEP = (JConstants.AVAILABLE_PROCESSORS << 3) + 1;

    private static final AtomicReferenceFieldUpdater<CopyOnWriteArrayList, Object[]> channelsUpdater =
            AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteArrayList.class, Object[].class, "array");
    private static final AtomicIntegerFieldUpdater<NettyChannelGroup> signalNeededUpdater =
            AtomicIntegerFieldUpdater.newUpdater(NettyChannelGroup.class, "signalNeeded");

    private final ConcurrentLinkedQueue<Runnable> waitAvailableListeners = new ConcurrentLinkedQueue<>();

    private final UnresolvedAddress address;

    private final CopyOnWriteArrayList<NettyChannel> channels = new CopyOnWriteArrayList<>();

    // 连接断开时自动被移除
    private final ChannelFutureListener remover = future -> remove(NettyChannel.attachChannel(future.channel()));

    private final IntSequence sequence = new IntSequence(DEFAULT_SEQUENCE_STEP);

    private final ConcurrentMap<String, Integer> weights = Maps.newConcurrentMap();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notifyCondition = lock.newCondition();
    // attempts to elide conditional wake-ups when the lock is uncontended.
    @SuppressWarnings("all")
    private volatile int signalNeeded = 0; // 0: false, 1: true

    private volatile boolean connecting = false;

    private volatile int capacity = Integer.MAX_VALUE;
    private volatile int warmUp = JConstants.DEFAULT_WARM_UP; // warm-up time
    private volatile long timestamp = SystemClock.millisClock().now();
    private volatile long deadlineMillis = -1;

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
            // snapshot of channels array
            Object[] elements = channelsUpdater.get(channels);
            int length = elements.length;
            if (length == 0) {
                if (waitForAvailable(1000)) { // wait a moment
                    continue;
                }
                throw new IllegalStateException("No channel");
            }
            if (length == 1) {
                return (JChannel) elements[0];
            }

            int index = sequence.next() & Integer.MAX_VALUE;

            return (JChannel) elements[index % length];
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean add(JChannel channel) {
        boolean added = channel instanceof NettyChannel && channels.add((NettyChannel) channel);
        if (added) {
            timestamp = SystemClock.millisClock().now(); // reset timestamp

            ((NettyChannel) channel).channel().closeFuture().addListener(remover);
            deadlineMillis = -1;

            if (signalNeededUpdater.getAndSet(this, 0) != 0) { // signal needed: true
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    notifyCondition.signalAll(); // must signal all
                } finally {
                    _look.unlock();
                }
            }

            notifyListeners();
        }
        return added;
    }

    @Override
    public boolean remove(JChannel channel) {
        boolean removed = channel instanceof NettyChannel && channels.remove(channel);
        if (removed) {
            timestamp = SystemClock.millisClock().now(); // reset timestamp

            if (channels.isEmpty()) {
                deadlineMillis = SystemClock.millisClock().now() + LOSS_INTERVAL;
            }
        }
        return removed;
    }

    @Override
    public int size() {
        return channels.size();
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isConnecting() {
        return connecting;
    }

    @Override
    public void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }

    @Override
    public boolean isAvailable() {
        return !channels.isEmpty();
    }

    @Override
    public boolean waitForAvailable(long timeoutMillis) {
        boolean available = isAvailable();
        if (available) {
            return true;
        }
        long remains = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

        final ReentrantLock _look = lock;
        _look.lock();
        try {
            // avoid "spurious wakeup" occurs
            while (!(available = isAvailable())) {
                signalNeeded = 1; // set signal needed to true
                if ((remains = notifyCondition.awaitNanos(remains)) <= 0) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            ThrowUtil.throwException(e);
        } finally {
            _look.unlock();
        }

        return available;
    }

    @Override
    public void onAvailable(Runnable listener) {
        waitAvailableListeners.add(listener);
        if (isAvailable()) {
            notifyListeners();
        }
    }

    @Override
    public int getWeight(Directory directory) {
        Requires.requireNotNull(directory, "directory");

        Integer weight = weights.get(directory.directoryString());
        return weight == null ? JConstants.DEFAULT_WEIGHT : weight;
    }

    @Override
    public void putWeight(Directory directory, int weight) {
        Requires.requireNotNull(directory, "directory");

        if (weight == JConstants.DEFAULT_WEIGHT) {
            // the default value does not need to be stored
            return;
        }
        weights.put(directory.directoryString(), weight > JConstants.MAX_WEIGHT ? JConstants.MAX_WEIGHT : weight);
    }

    @Override
    public void removeWeight(Directory directory) {
        Requires.requireNotNull(directory, "directory");

        weights.remove(directory.directoryString());
    }

    @Override
    public int getWarmUp() {
        return warmUp > 0 ? warmUp : 0;
    }

    @Override
    public void setWarmUp(int warmUp) {
        this.warmUp = warmUp;
    }

    @Override
    public boolean isWarmUpComplete() {
        return SystemClock.millisClock().now() - timestamp > warmUp;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public long deadlineMillis() {
        return deadlineMillis;
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
        return "NettyChannelGroup{" +
                "address=" + address +
                ", channels=" + channels +
                ", weights=" + weights +
                ", warmUp=" + warmUp +
                ", timestamp=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new Date(timestamp)) +
                ", deadlineMillis=" + deadlineMillis +
                '}';
    }

    void notifyListeners() {
        for (;;) {
            Runnable listener = waitAvailableListeners.poll();
            if (listener == null) {
                break;
            }
            listener.run();
        }
    }
}
