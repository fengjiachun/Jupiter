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

package org.jupiter.rpc.consumer.future;

import org.jupiter.common.util.Signal;
import org.jupiter.common.util.internal.JUnsafe;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public abstract class AbstractFuture<V> {

    @SuppressWarnings("all")
    protected static final Signal TIMEOUT = Signal.valueOf(AbstractFuture.class, "time_out");

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
        return state != NEW;
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
            completion(v);
        }
    }

    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            // putOrderedInt在JIT后会通过intrinsic优化掉StoreLoad屏障, 不保证可见性
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            completion(t);
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
    private void completion(Object x) {
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
    private int awaitDone(boolean timed, long nanos) {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
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
            } else if (q == null) { // 创建一个等待节点
                q = new WaitNode();
            } else if (!queued) { // 将当前等待节点入队
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            } else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) { // 设置超时, 阻塞当前线程(阻塞指定时间)
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
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

    // unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE = JUnsafe.getUnsafe();
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
