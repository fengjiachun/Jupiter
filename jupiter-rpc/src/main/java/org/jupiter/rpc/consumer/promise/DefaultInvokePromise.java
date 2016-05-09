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

package org.jupiter.rpc.consumer.promise;

import org.jupiter.common.util.Maps;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.exception.RemoteException;
import org.jupiter.rpc.exception.TimeoutException;
import org.jupiter.rpc.model.metadata.ResultWrapper;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.DispatchType.ROUND;
import static org.jupiter.rpc.JListener.JResult;
import static org.jupiter.rpc.Status.*;

/**
 * The default implementation of {@link InvokePromise}, based on {@link ReentrantLock}.
 *
 * jupiter
 * org.jupiter.rpc.consumer.promise
 *
 * @author jiachun.fjc
 */
public class DefaultInvokePromise extends InvokePromise<Object> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultInvokePromise.class);

    // 单播场景的promise, Long作为Key hashCode和equals效率都更高
    private static final ConcurrentMap<Long, DefaultInvokePromise> roundPromises = Maps.newConcurrentHashMap();
    // 组播场景的promise, 组播都是一个invokeId, 所以要把Key再加一个前缀
    private static final ConcurrentMap<String, DefaultInvokePromise> broadcastPromises = Maps.newConcurrentHashMap();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition doneCondition = lock.newCondition();

    private final long invokeId; // request id, 组播的场景可重复
    private final JChannel channel;
    private final JRequest request;
    private final int timeoutMillis;
    private final long startTimestamp = SystemClock.millisClock().now();

    private volatile long sentTimestamp;
    private volatile JResponse response;
    private volatile JListener listener;
    private volatile ConsumerHook[] hooks;

    public DefaultInvokePromise(JChannel channel, JRequest request, int timeoutMillis) {
        this(channel, request, timeoutMillis, ROUND);
    }

    public DefaultInvokePromise(JChannel channel, JRequest request, int timeoutMillis, DispatchType dispatchType) {
        invokeId = request.invokeId();
        this.channel = channel;
        this.request = request;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT;

        if (dispatchType == BROADCAST) {
            broadcastPromises.put(broadcastChildInvokeId(channel, invokeId), this);
        } else {
            roundPromises.put(invokeId, this);
        }
    }

    public static boolean received(JChannel channel, JResponse response) {
        long invokeId = response.id();
        // 在不知道是组播还是单播的情况下需要组播做出性能让步, 查询两次Map
        DefaultInvokePromise promise = roundPromises.remove(invokeId);
        if (promise == null) {
            promise = broadcastPromises.remove(broadcastChildInvokeId(channel, invokeId));
        }
        if (promise == null) {
            logger.warn("A timeout response [{}] finally returned on {}.", response, channel);
            return false;
        }

        promise.doReceived(response);
        return true;
    }

    public static String broadcastChildInvokeId(JChannel channel, long invokeId) {
        return channel.id() + invokeId;
    }

    @Override
    public DefaultInvokePromise hooks(ConsumerHook[] hooks) {
        this.hooks = hooks;
        return this;
    }

    @Override
    public DefaultInvokePromise listener(JListener listener) {
        if (listener == null) {
            return this;
        }

        if (isDone()) {
            notifyListener(listener);
            return this;
        }

        final ReentrantLock _lock = lock;
        _lock.lock();
        try {
            if (!isDone()) {
                this.listener = listener;
                return this;
            }
        } finally {
            _lock.unlock();
        }

        notifyListener(listener);
        return this;
    }

    @Override
    public void chalkUpSentTimestamp() {
        sentTimestamp = SystemClock.millisClock().now();
    }

    @Override
    public Object getResult() throws Throwable {
        if (!isDone()) {
            long start = System.nanoTime();
            final ReentrantLock _lock = lock;
            _lock.lock();
            try {
                while (!isDone()) {
                    doneCondition.await(timeoutMillis, MILLISECONDS);

                    if (isDone() || (System.nanoTime() - start) > MILLISECONDS.toNanos(timeoutMillis)) {
                        break;
                    }
                }
            } finally {
                _lock.unlock();
            }

            if (!isDone()) {
                throw new TimeoutException(channel.remoteAddress(), sentTimestamp > 0 ? SERVER_TIMEOUT : CLIENT_TIMEOUT);
            }
        }
        return resultFromResponse();
    }

    public boolean isDone() {
        return response != null;
    }

    private void doReceived(JResponse response) {
        this.response = response;

        // If there is a listener, that is considered to be an asynchronous call,
        // and attempts to elide conditional wake-ups when the lock is uncontended.
        if (listener != null) {
            notifyListener(listener);
        } else {
            final ReentrantLock _lock = lock;
            _lock.lock();
            try {
                doneCondition.signal();
            } finally {
                _lock.unlock();
            }

            complete();
        }

        // call hook's after method
        if (hooks != null) {
            for (ConsumerHook h : hooks) {
                h.after(request, channel);
            }
        }
    }

    private Object resultFromResponse() throws Throwable {
        final JResponse _response = this.response;
        byte status = _response.status();
        if (status == OK.value()) {
            ResultWrapper wrapper = _response.result();
            return wrapper.getResult();
        }

        throw new RemoteException(_response.toString(), channel.remoteAddress());
    }

    private void complete() {
        final JResponse _response = this.response;
        byte status = _response.status();
        ResultWrapper wrapper = _response.result();
        if (status == OK.value()) {
            try {
                resolve(wrapper.getResult());
            } catch (Throwable t) {
                reject(t);
            }
        } else {
            reject(new RemoteException(_response.toString(), channel.remoteAddress()));
        }
    }

    private void notifyListener(JListener listener) {
        final JResponse _response = this.response;
        byte status = _response.status();
        ResultWrapper wrapper = _response.result();
        if (status == OK.value()) {
            try {
                JResult result = new JResult(channel.remoteAddress(), wrapper.getResult());
                listener.complete(request, result);
            } catch (Throwable t) {
                listener.failure(request, t);
            }
        } else {
            listener.failure(request, new RemoteException(_response.toString(), channel.remoteAddress()));
        }
    }

    /**
     * Timeout scanner.
     */
    private static class TimeoutScanner implements Runnable {

        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            for (;;) {
                try {
                    // 单播
                    for (DefaultInvokePromise promise : roundPromises.values()) {
                        if (promise == null || promise.isDone()) {
                            continue;
                        }
                        if (SystemClock.millisClock().now() - promise.startTimestamp > promise.timeoutMillis) {
                            processingTimeoutFuture(promise);
                        }
                    }

                    // 组播
                    for (DefaultInvokePromise promise : broadcastPromises.values()) {
                        if (promise == null || promise.isDone()) {
                            continue;
                        }
                        if (SystemClock.millisClock().now() - promise.startTimestamp > promise.timeoutMillis) {
                            processingTimeoutFuture(promise);
                        }
                    }

                    Thread.sleep(30);
                } catch (Throwable t) {
                    logger.error("An exception has been caught while scanning the timeout invocations {}.", t);
                }
            }
        }

        private void processingTimeoutFuture(DefaultInvokePromise promise) {
            ResultWrapper result = new ResultWrapper();
            Status status = promise.sentTimestamp > 0 ? SERVER_TIMEOUT : CLIENT_TIMEOUT;
            result.setError(new TimeoutException(promise.channel.remoteAddress(), status));

            JResponse r = JResponse.newInstance(promise.invokeId, status, result);
            DefaultInvokePromise.received(promise.channel, r);
        }
    }

    static {
        Thread t = new Thread(new TimeoutScanner(), "timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
