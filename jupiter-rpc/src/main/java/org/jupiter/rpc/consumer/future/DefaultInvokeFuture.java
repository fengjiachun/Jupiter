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

import org.jupiter.common.util.Maps;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.UnsafeAccess;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.DispatchMode;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.Response;
import org.jupiter.rpc.aop.ConsumerHook;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.error.RemoteException;
import org.jupiter.rpc.error.TimeoutException;
import org.jupiter.rpc.model.metadata.ResultWrapper;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.rpc.DispatchMode.BROADCAST;
import static org.jupiter.rpc.DispatchMode.ROUND;
import static org.jupiter.rpc.Status.*;

/**
 * The default implementation of {@link InvokeFuture}, based on {@link ReentrantLock}.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public class DefaultInvokeFuture implements InvokeFuture {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultInvokeFuture.class);

    // 单播场景的future, Long作为Key hashCode和equals效率都更高
    private static final ConcurrentMap<Long, DefaultInvokeFuture> roundFutures = Maps.newConcurrentHashMap();
    // 组播场景的future, 组播都是一个invokeId, 所以要把Key再加一个前缀
    private static final ConcurrentMap<String, DefaultInvokeFuture> broadcastFutures = Maps.newConcurrentHashMap();

    private final long invokeId; // request id, 组播的场景可重复
    private final String id; // 组播场景使用的 id
    private final JChannel channel;
    private final Request request;
    private final int timeoutMillis;
    private final DispatchMode mode;

    private volatile Response response;
    private volatile JListener listener;
    private volatile List<ConsumerHook> hooks;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition done = lock.newCondition();
    private final long start = SystemClock.millisClock().now();
    private volatile long sent;

    public DefaultInvokeFuture(JChannel channel, Request request, int timeoutMillis) {
        this(channel, request, timeoutMillis, ROUND);
    }

    public DefaultInvokeFuture(JChannel channel, Request request, int timeoutMillis, DispatchMode mode) {
        invokeId = request.invokeId();
        this.channel = channel;
        this.request = request;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT;
        this.mode = mode;

        if (mode == BROADCAST) {
            id = generateGroupChildId(channel, invokeId);
            broadcastFutures.put(id, this);
        } else {
            id = null;
            roundFutures.put(invokeId, this);
        }
    }

    public static boolean received(JChannel channel, Response response) {
        long invokeId = response.id();
        // 在不知道是组播还是单播的情况下需要组播做出性能让步, 查询两次Map
        DefaultInvokeFuture future = roundFutures.remove(invokeId);
        if (future == null) {
            future = broadcastFutures.remove(generateGroupChildId(channel, invokeId));
        }
        if (future != null) {
            future.doReceived(response);
            return true;
        }

        logger.warn("A timeout response [{}] finally returned on {}.", response, channel);

        return false;
    }

    public static boolean received(JChannel channel, Response response, DispatchMode mode) {
        DefaultInvokeFuture future;
        if (mode == BROADCAST) {
            future = broadcastFutures.remove(generateGroupChildId(channel, response.id()));
        } else {
            future = roundFutures.remove(response.id());
        }
        if (future != null) {
            future.doReceived(response);
            return true;
        }

        logger.warn("A timeout response [{}] finally returned on {}.", response, channel);

        return false;
    }

    public static String generateGroupChildId(JChannel channel, long invokeId) {
        return channel.id() + invokeId;
    }

    @Override
    public DefaultInvokeFuture hooks(List<ConsumerHook> hooks) {
        this.hooks = hooks;
        return this;
    }

    @Override
    public DefaultInvokeFuture listener(JListener listener) {
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
    public void sent() {
        sent = SystemClock.millisClock().now();
    }

    @Override
    public Object singleResult() throws Throwable {
        if (!isDone()) {
            long start = System.nanoTime();
            final ReentrantLock _lock = lock;
            _lock.lock();
            try {
                while (!isDone()) {
                    done.await(timeoutMillis, MILLISECONDS);
                    if (isDone() || (System.nanoTime() - start) > MILLISECONDS.toNanos(timeoutMillis)) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                UnsafeAccess.UNSAFE.throwException(e);
            } finally {
                _lock.unlock();
            }
            if (!isDone()) {
                throw new TimeoutException(channel.remoteAddress(), sent > 0 ? SERVER_TIMEOUT.value() : CLIENT_TIMEOUT.value());
            }
        }
        return resultFromResponse();
    }

    public String getId() {
        if (mode == BROADCAST) {
            return id;
        }
        return String.valueOf(invokeId);
    }

    public boolean isDone() {
        return response != null;
    }

    public JChannel getChannel() {
        return channel;
    }

    private void doReceived(Response response) {
        // If there is a listener, that is considered to be an asynchronous call,
        // and attempts to elide conditional wake-ups when the lock is uncontended.
        if (listener != null) {
            notifyListener(listener);
        } else {
            final ReentrantLock _lock = lock;
            _lock.lock();
            try {
                this.response = response;
                done.signal();
            } finally {
                _lock.unlock();
            }
        }

        // call hook's after method
        if (hooks != null) {
            for (ConsumerHook h : hooks) {
                h.after(request);
            }
        }
    }

    private Object resultFromResponse() throws Throwable {
        Response _response = this.response;
        byte status = _response.status();
        if (status == OK.value()) {
            ResultWrapper wrapper = _response.result();
            return wrapper.getResult();
        }

        Throwable t = _response.result().getError();
        throw t != null ? t : new RemoteException(_response.toString(), channel.remoteAddress());
    }

    private void notifyListener(JListener responseListener) {
        Response _response = this.response;
        byte status = _response.status();
        ResultWrapper wrapper = _response.result();
        if (status == OK.value()) {
            try {
                listener.complete(request, wrapper.getResult());
            } catch (Exception e) {
                listener.failure(request, e);
            }
        } else {
            Throwable t = wrapper.getError();
            listener.failure(request, t != null ? t : new RemoteException(_response.toString(), channel.remoteAddress()));
        }
    }

    /**
     * 超时扫描
     */
    private static class InvokeTimeoutScanner implements Runnable {

        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            for (;;) {
                try {
                    // 单播
                    for (DefaultInvokeFuture future : roundFutures.values()) {
                        if (future == null || future.isDone()) {
                            continue;
                        }
                        if (SystemClock.millisClock().now() - future.start > future.timeoutMillis) {
                            dealWithTimeout(future);
                        }
                    }

                    // 组播
                    for (DefaultInvokeFuture future : broadcastFutures.values()) {
                        if (future == null || future.isDone()) {
                            continue;
                        }
                        if (SystemClock.millisClock().now() - future.start > future.timeoutMillis) {
                            dealWithTimeout(future);
                        }
                    }

                    Thread.sleep(30);
                } catch (Exception e) {
                    logger.error("An exception has been caught while scanning the timeout invocations {}.", e);
                }
            }
        }

        private void dealWithTimeout(DefaultInvokeFuture future) {
            Response timeoutResponse = new Response(future.invokeId);
            ResultWrapper result = new ResultWrapper();
            byte status = future.sent > 0 ? SERVER_TIMEOUT.value() : CLIENT_TIMEOUT.value();
            result.setError(new TimeoutException(future.channel.remoteAddress(), status));

            // 设置超时状态
            timeoutResponse.status(status);
            timeoutResponse.result(result);
            DefaultInvokeFuture.received(future.channel, timeoutResponse);
        }
    }

    static {
        Thread t = new Thread(new InvokeTimeoutScanner(), "invocation.timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
