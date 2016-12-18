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
import org.jupiter.common.util.Signal;
import org.jupiter.common.util.internal.JUnsafe;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.exception.BizException;
import org.jupiter.rpc.exception.RemoteException;
import org.jupiter.rpc.exception.TimeoutException;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.transport.channel.JChannel;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.rpc.ConsumerHook.*;
import static org.jupiter.rpc.DispatchType.ROUND;
import static org.jupiter.transport.Status.*;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("all")
public class InvokeFuture<V> extends Future<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(InvokeFuture.class);

    private static final Signal C_TIMEOUT = Signal.valueOf(InvokeFuture.class, "client_time_out");
    private static final Signal S_TIMEOUT = Signal.valueOf(InvokeFuture.class, "server_time_out");

    private static final long DEFAULT_TIMEOUT_NANOSECONDS = MILLISECONDS.toNanos(DEFAULT_TIMEOUT);

    private static final ConcurrentMap<Long, InvokeFuture<?>> roundFutures = Maps.newConcurrentMapLong();
    private static final ConcurrentMap<String, InvokeFuture<?>> broadcastFutures = Maps.newConcurrentMap();

    private final long invokeId; // request id, 组播的场景可重复
    private final JChannel channel;
    private final Class<V> returnType;
    private final long timeout;
    private final long startTime = System.nanoTime();

    private volatile boolean sent = false;

    private ConsumerHook[] hooks = EMPTY_HOOKS;
    private Object listeners;

    public InvokeFuture(long invokeId, JChannel channel, Class<V> returnType, long timeoutMillis) {
        this(invokeId, channel, returnType, timeoutMillis, ROUND);
    }

    public InvokeFuture(long invokeId, JChannel channel, Class<V> returnType, long timeoutMillis, DispatchType dispatchType) {
        this.invokeId = invokeId;
        this.channel = channel;
        this.returnType = returnType;
        this.timeout = timeoutMillis > 0 ? MILLISECONDS.toNanos(timeoutMillis) : DEFAULT_TIMEOUT_NANOSECONDS;

        switch (dispatchType) {
            case ROUND:
                roundFutures.put(invokeId, this);
                break;
            case BROADCAST:
                broadcastFutures.put(subInvokeId(channel, invokeId), this);
                break;
            default:
                throw new IllegalArgumentException("unsupported " + dispatchType);
        }
    }

    public V getResult() throws Exception {
        try {
            return get(timeout, NANOSECONDS);
        } catch (Signal s) {
            SocketAddress address = channel.remoteAddress();
            if (C_TIMEOUT == s) {
                throw new TimeoutException(address, CLIENT_TIMEOUT);
            } else if (S_TIMEOUT == s) {
                throw new TimeoutException(address, SERVER_TIMEOUT);
            } else {
                throw new RemoteException(s.name(), address);
            }
        } catch (Throwable t) {
            JUnsafe.throwException(t);
        }
        return null; // never get here
    }

    public Class<V> returnType() {
        return returnType;
    }

    public InvokeFuture<V> markSent() {
        sent = true;
        return this;
    }

    public ConsumerHook[] hooks() {
        return hooks;
    }

    public InvokeFuture<V> hooks(ConsumerHook[] hooks) {
        checkNotNull(hooks, "hooks");

        this.hooks = hooks;
        return this;
    }

    public InvokeFuture<V> addListener(JListener<V> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            addListener0(listener);
        }

        if (isDone()) {
            notifyListeners(state(), outcome());
        }

        return this;
    }

    public InvokeFuture<V> addListeners(JListener<V>... listeners) {
        checkNotNull(listeners, "listeners");

        synchronized (this) {
            for (JListener<V> listener : listeners) {
                if (listener == null) {
                    continue;
                }
                addListener0(listener);
            }
        }

        if (isDone()) {
            notifyListeners(state(), outcome());
        }

        return this;
    }

    public InvokeFuture<V> removeListener(JListener<V> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            removeListener0(listener);
        }

        return this;
    }

    public InvokeFuture<V> removeListeners(JListener<V>... listeners) {
        checkNotNull(listeners, "listeners");

        synchronized (this) {
            for (JListener<V> listener : listeners) {
                if (listener == null) {
                    continue;
                }
                removeListener0(listener);
            }
        }

        return this;
    }

    @Override
    protected void done(int state, Object x) {
        notifyListeners(state, x);
    }

    private void addListener0(JListener<V> listener) {
        if (listeners == null) {
            listeners = listener;
        } else if (listeners instanceof DefaultListeners) {
            ((DefaultListeners<V>) listeners).add(listener);
        } else {
            listeners = new DefaultListeners<>((JListener<V>) listeners, listener);
        }
    }

    private void removeListener0(JListener<V> listener) {
        if (listeners instanceof DefaultListeners) {
            ((DefaultListeners<V>) listeners).remove(listener);
        } else if (listeners == listener) {
            listeners = null;
        }
    }

    private void notifyListeners(int state, Object x) {
        Object listeners;
        synchronized (this) {
            // no competition unless the listener is added too late or the rpc call timeout
            if (this.listeners == null) {
                return;
            }

            listeners = this.listeners;
            this.listeners = null;
        }

        if (listeners instanceof DefaultListeners) {
            JListener<V>[] array = ((DefaultListeners<V>) listeners).listeners();
            int size = ((DefaultListeners<V>) listeners).size();

            for (int i = 0; i < size; i++) {
                notifyListener0(array[i], state, x);
            }
        } else {
            notifyListener0((JListener<Object>) listeners, state, x);
        }
    }

    private static <V> void notifyListener0(JListener<V> listener, int state, Object x) {
        try {
            if (NORMAL == state) {
                listener.complete((V) x);
            } else {
                listener.failure((Throwable) x);
            }
        } catch (Throwable t) {
            logger.error("An exception was thrown by {}.{}.",
                    listener.getClass().getName(), NORMAL == state ? "complete()" : "failure()", stackTrace(t));
        }
    }

    private void doReceived(JResponse response) {
        byte status = response.status();
        if (OK.value() == status) {
            ResultWrapper wrapper = response.result();
            set((V) wrapper.getResult());
        } else if (CLIENT_TIMEOUT.value() == status) {
            setException(C_TIMEOUT);
        } else if (SERVER_TIMEOUT.value() == status) {
            setException(S_TIMEOUT);
        } else if (SERVICE_ERROR.value() == status) {
            setException(new BizException(response.toString(), channel.remoteAddress()));
        } else {
            setException(new RemoteException(response.toString(), channel.remoteAddress()));
        }

        // call hook's after method
        for (int i = 0; i < hooks.length; i++) {
            hooks[i].after(response, channel);
        }
    }

    public static void received(JChannel channel, JResponse response) {
        long invokeId = response.id();
        // 在不知道是组播还是单播的情况下需要组播做出性能让步, 查询两次Map
        InvokeFuture<?> future = roundFutures.remove(invokeId);
        if (future == null) {
            future = broadcastFutures.remove(subInvokeId(channel, invokeId));
        }
        if (future == null) {
            logger.warn("A timeout response [{}] finally returned on {}.", response, channel);
            return;
        }

        future.doReceived(response);
    }

    private static String subInvokeId(JChannel channel, long invokeId) {
        return channel.id() + invokeId;
    }

    // timeout scanner
    @SuppressWarnings("all")
    private static class TimeoutScanner implements Runnable {

        public void run() {
            for (;;) {
                try {
                    // round
                    for (InvokeFuture<?> future : roundFutures.values()) {
                        process(future);
                    }

                    // broadcast
                    for (InvokeFuture<?> future : broadcastFutures.values()) {
                        process(future);
                    }

                    Thread.sleep(30);
                } catch (Throwable t) {
                    logger.error("An exception has been caught while scanning the timeout futures {}.", t);
                }
            }
        }

        private void process(InvokeFuture<?> future) {
            if (future == null || future.isDone()) {
                return;
            }

            if (System.nanoTime() - future.startTime > future.timeout) {
                JResponse response = new JResponse(future.invokeId);
                response.status(future.sent ? SERVER_TIMEOUT : CLIENT_TIMEOUT);

                InvokeFuture.received(future.channel, response);
            }
        }
    }

    static {
        Thread t = new Thread(new TimeoutScanner(), "timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
