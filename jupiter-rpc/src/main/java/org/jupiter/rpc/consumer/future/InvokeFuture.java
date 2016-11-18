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

import org.jupiter.common.concurrent.atomic.AtomicUpdater;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Signal;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.JUnsafe;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.exception.RemoteException;
import org.jupiter.rpc.exception.TimeoutException;
import org.jupiter.rpc.model.metadata.ResultWrapper;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.DispatchType.ROUND;
import static org.jupiter.rpc.Status.*;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("all")
public class InvokeFuture<V> extends Future<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(InvokeFuture.class);

    private static final Signal C_TIMEOUT_SIGNAL = Signal.valueOf(InvokeFuture.class, "client_time_out");
    private static final Signal S_TIMEOUT_SIGNAL = Signal.valueOf(InvokeFuture.class, "server_time_out");

    private static final AtomicReferenceFieldUpdater<CopyOnWriteArrayList, Object[]> listenersGetter =
            AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteArrayList.class, Object[].class, "array");

    // 单播场景的future, Long作为Key hashCode和equals效率都更高
    private static final ConcurrentMap<Long, InvokeFuture<?>> roundFutures;
    // 组播场景的future, 组播都是一个invokeId, 所以要把Key再加一个前缀
    private static final ConcurrentMap<String, InvokeFuture<?>> broadcastFutures;

    static {
        boolean useNonBlockingHash = SystemPropertyUtil.getBoolean("jupiter.future.non_blocking_hash", false);
        if (useNonBlockingHash) {
            roundFutures = Maps.newNonBlockingHashMapLong();
            broadcastFutures = Maps.newNonBlockingHashMap();
        } else {
            roundFutures = Maps.newConcurrentHashMap();
            broadcastFutures = Maps.newConcurrentHashMap();
        }
    }

    private final long invokeId; // request id, 组播的场景可重复
    private final JChannel channel;
    private final JRequest request;
    private final long timeout;
    private final long startTime = System.nanoTime();
    private final CopyOnWriteArrayList<JListener<V>> listeners = new CopyOnWriteArrayList<>();
    private final Class<V> returnType;

    private volatile long sentTime;
    private volatile ConsumerHook[] hooks;

    public InvokeFuture(JChannel channel, JRequest request, Class<V> returnType, long timeoutMillis) {
        this(channel, request, returnType, timeoutMillis, ROUND);
    }

    public InvokeFuture(JChannel channel, JRequest request, Class<V> returnType, long timeoutMillis, DispatchType dispatchType) {
        invokeId = request.invokeId();
        this.channel = channel;
        this.request = request;
        this.returnType = returnType;
        this.timeout = timeoutMillis <= 0
                ? MILLISECONDS.toNanos(DEFAULT_TIMEOUT) : MILLISECONDS.toNanos(timeoutMillis);

        if (dispatchType == BROADCAST) {
            broadcastFutures.put(subInvokeId(channel, invokeId), this);
        } else {
            roundFutures.put(invokeId, this);
        }
    }

    public V getResult() throws Exception {
        try {
            return get(timeout, NANOSECONDS);
        } catch (Signal s) {
            if (C_TIMEOUT_SIGNAL == s) {
                throw new TimeoutException(channel.remoteAddress(), CLIENT_TIMEOUT);
            } else if (S_TIMEOUT_SIGNAL == s) {
                throw new TimeoutException(channel.remoteAddress(), SERVER_TIMEOUT);
            }
            return null; // never get here
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            JUnsafe.throwException(t);
        }
        return null; // never get here
    }

    public Class<V> getReturnType() {
        return returnType;
    }

    public InvokeFuture<V> setSentTime() {
        sentTime = System.nanoTime();
        return this;
    }

    public InvokeFuture<V> hooks(ConsumerHook[] hooks) {
        this.hooks = hooks;
        return this;
    }

    public InvokeFuture<V> addListener(JListener<V> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    protected void done(int state, Object x) {
        if (!listeners.isEmpty()) {
            try {
                Object[] array = listenersGetter.get(listeners);
                if (NORMAL == state) {
                    for (int i = 0; i < array.length; i++) {
                        ((JListener<V>) array[i]).complete((V) x);
                    }
                } else {
                    for (int i = 0; i < array.length; i++) {
                        ((JListener<V>) array[i]).failure((Throwable) x);
                    }
                }
            } catch (Throwable t) {
                logger.error("An exception has been caught on calling listeners {}.", stackTrace(t));
            }
        }
    }

    private void doReceived(JResponse response) {
        byte status = response.status();
        if (OK.value() == status) {
            ResultWrapper wrapper = response.result();
            set((V) wrapper.getResult());
        } else if (CLIENT_TIMEOUT.value() == status) {
            setException(C_TIMEOUT_SIGNAL);
        } else if (SERVER_TIMEOUT.value() == status) {
            setException(S_TIMEOUT_SIGNAL);
        } else {
            setException(new RemoteException(response.toString(), channel.remoteAddress()));
        }

        // call hook's after method
        if (hooks != null) {
            for (ConsumerHook h : hooks) {
                h.after(request, response, channel);
            }
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
                        if (future == null || future.isDone()) {
                            continue;
                        }
                        if (System.nanoTime() - future.startTime > future.timeout) {
                            InvokeFuture.received(
                                    future.channel,
                                    JResponse.newInstance(
                                            future.invokeId,
                                            future.request.serializerCode(),
                                            future.sentTime > 0 ? SERVER_TIMEOUT : CLIENT_TIMEOUT
                                    )
                            );
                        }
                    }

                    // broadcast
                    for (InvokeFuture<?> future : broadcastFutures.values()) {
                        if (future == null || future.isDone()) {
                            continue;
                        }
                        if (System.nanoTime() - future.startTime > future.timeout) {
                            InvokeFuture.received(
                                    future.channel,
                                    JResponse.newInstance(
                                            future.invokeId,
                                            future.request.serializerCode(),
                                            future.sentTime > 0 ? SERVER_TIMEOUT : CLIENT_TIMEOUT
                                    )
                            );
                        }
                    }

                    Thread.sleep(30);
                } catch (Throwable t) {
                    logger.error("An exception has been caught while scanning the timeout futures {}.", t);
                }
            }
        }
    }

    static {
        Thread t = new Thread(new TimeoutScanner(), "timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
