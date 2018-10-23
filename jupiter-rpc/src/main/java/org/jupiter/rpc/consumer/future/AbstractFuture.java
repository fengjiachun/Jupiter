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

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.jupiter.rpc.consumer.future;

import org.jupiter.common.util.Signal;
import org.jupiter.common.util.internal.UnsafeUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * Forked from java.util.concurrent.FutureTask
 *
 * @author jiachun.fjc
 */
public abstract class AbstractFuture<V> {

    @SuppressWarnings("all")
    protected static final Signal TIMEOUT = Signal.valueOf(AbstractFuture.class, "TIMEOUT");

    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     */
    protected static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000L;

    /**
     * 内部状态转换过程:
     * NEW -> COMPLETING -> NORMAL          // 正常完成
     * NEW -> COMPLETING -> EXCEPTIONAL     // 出现异常
     */
    private volatile int state;
    protected static final int NEW = 0;
    protected static final int COMPLETING = 1;
    protected static final int NORMAL = 2;
    protected static final int EXCEPTIONAL = 3;

    // 正常返回结果或者异常对象, 通过get()获取或者抛出异常, 无volatile修饰, 通过state保证可见性
    private Object outcome;
    // 存放等待线程的Treiber stack
    @SuppressWarnings("unused")
    private volatile WaitNode waiters;

    public AbstractFuture() {
        this.state = NEW;
    }

    public boolean isDone() {
        // state can not be COMPLETING, because of method AbstractFuture#outcome()
        //
        // see https://github.com/fengjiachun/Jupiter/issues/55
        return state > COMPLETING;
    }

    protected int state() {
        return state;
    }

    /**
     * 调用这个方法之前, 需要先读 {@code state} 来保证可见性
     */
    protected Object outcome() {
        return outcome;
    }

    protected V get() throws Throwable {
        int s = state;
        if (s <= COMPLETING) {
            s = awaitDone(false, 0L);
        }
        return report(s);
    }

    protected V get(long timeout, TimeUnit unit) throws Throwable {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        int s = state;
        if (s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) {
            throw TIMEOUT;
        }
        return report(s);
    }

    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            // putOrderedInt在JIT后会通过intrinsic优化掉StoreLoad屏障, 不保证可见性
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            finishCompletion(v);
        }
    }

    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            // putOrderedInt在JIT后会通过intrinsic优化掉StoreLoad屏障, 不保证可见性
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion(t);
        }
    }

    protected abstract void done(int state, Object x);

    /**
     * 返回正常执行结果或者异常
     *
     * @param s 状态值
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws Throwable {
        Object x = outcome;
        if (s == NORMAL) {
            return (V) x;
        }
        throw (Throwable) x;
    }

    /**
     * 1. 唤醒并移除Treiber Stack中所有等待线程
     * 2. 调用钩子函数done()
     */
    private void finishCompletion(Object x) {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null; ) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null) {
                        break;
                    }
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done(state, x);
    }

    /**
     * 等待任务完成或者超时
     */
    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        // The code below is very delicate, to achieve these goals:
        // - call nanoTime exactly once for each call to park
        // - if nanos <= 0L, return promptly without allocation or nanoTime
        // - if nanos == Long.MIN_VALUE, don't underflow
        // - if nanos == Long.MAX_VALUE, and nanoTime is non-monotonic
        //   and we suffer a spurious wakeup, we will do no worse than
        //   to park-spin for a while
        long startTime = 0L;    // special value 0L means not yet parked
        WaitNode q = null;
        boolean queued = false;
        for (;;) {
            int s = state;
            if (s > COMPLETING) { // 任务执行完成
                if (q != null) {
                    q.thread = null;
                }
                return s; // 返回任务状态
            } else if (s == COMPLETING) { // 正在完成中, 让出CPU
                Thread.yield();
            } else if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            } else if (q == null) { // 创建一个等待节点
                if (timed && nanos <= 0L) {
                    return s;
                }
                q = new WaitNode();
            } else if (!queued) { // 将当前等待节点入队
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            } else if (timed) {
                final long parkNanos;
                if (startTime == 0L) { // first time
                    startTime = System.nanoTime();
                    if (startTime == 0L) {
                        startTime = 1L;
                    }
                    parkNanos = nanos;
                } else {
                    long elapsed = System.nanoTime() - startTime;
                    if (elapsed >= nanos) {
                        removeWaiter(q);
                        return state;
                    }
                    parkNanos = nanos - elapsed;
                }
                // the number of nanoseconds for which it is faster to spin
                // rather than to use timed park.
                if (parkNanos > SPIN_FOR_TIMEOUT_THRESHOLD
                        && state < COMPLETING) {
                    LockSupport.parkNanos(this, parkNanos);
                }
            } else { // 直接阻塞当前线程
                LockSupport.park(this);
            }
        }
    }

    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            // 将node从等待队列中移除, 以node.thread == null为依据, 发生竞争则重试
            retry:
            for (;;) { // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null) {
                        pred = q;
                    } else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) { // check for race
                            continue retry;
                        }
                    } else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s)) {
                        continue retry;
                    }
                }
                break;
            }
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Treiber_Stack
     */
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;

        WaitNode() {
            thread = Thread.currentThread();
        }
    }

    @Override
    public String toString() {
        final String status;
        switch (state) {
            case NORMAL:
                status = "[Completed normally]";
                break;
            case EXCEPTIONAL:
                status = "[Completed exceptionally: " + outcome + "]";
                break;
            default:
                status = "[Not completed]";
        }
        return super.toString() + status;
    }

    // unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE = UnsafeUtil.getUnsafe();
    private static final long stateOffset;
    private static final long waitersOffset;

    static {
        try {
            Class<?> k = AbstractFuture.class;
            stateOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("state"));
            waitersOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
