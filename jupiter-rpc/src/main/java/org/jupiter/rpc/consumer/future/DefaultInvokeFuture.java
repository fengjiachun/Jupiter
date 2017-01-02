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
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.ConsumerHook;
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.exception.JupiterBizException;
import org.jupiter.rpc.exception.JupiterRemoteException;
import org.jupiter.rpc.exception.JupiterTimeoutException;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.transport.channel.JChannel;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.rpc.ConsumerHook.EMPTY_HOOKS;
import static org.jupiter.transport.Status.*;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("all")
public class DefaultInvokeFuture<V> extends AbstractInvokeFuture<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultInvokeFuture.class);

    private static final Signal C_TIMEOUT = Signal.valueOf(DefaultInvokeFuture.class, "client_time_out");
    private static final Signal S_TIMEOUT = Signal.valueOf(DefaultInvokeFuture.class, "server_time_out");

    private static final long DEFAULT_TIMEOUT_NANOSECONDS = MILLISECONDS.toNanos(DEFAULT_TIMEOUT);

    private static final ConcurrentMap<Long, DefaultInvokeFuture<?>> roundFutures = Maps.newConcurrentMapLong();
    private static final ConcurrentMap<String, DefaultInvokeFuture<?>> broadcastFutures = Maps.newConcurrentMap();

    private final long invokeId; // request id, 组播的场景可重复
    private final JChannel channel;
    private final Class<V> returnType;
    private final long timeout;
    private final long startTime = System.nanoTime();

    private volatile boolean sent = false;

    private ConsumerHook[] hooks = EMPTY_HOOKS;

    public static <T> DefaultInvokeFuture<T> with(long invokeId, JChannel channel, Class<T> returnType, long timeoutMillis, DispatchType dispatchType) {
        return new DefaultInvokeFuture<T>(invokeId, channel, returnType, timeoutMillis, dispatchType);
    }

    private DefaultInvokeFuture(long invokeId, JChannel channel, Class<V> returnType, long timeoutMillis, DispatchType dispatchType) {
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

    @Override
    public Class<V> returnType() {
        return returnType;
    }

    @Override
    public V getResult() throws Throwable {
        try {
            return get(timeout, NANOSECONDS);
        } catch (Signal s) {
            SocketAddress address = channel.remoteAddress();
            if (C_TIMEOUT == s) {
                throw new JupiterTimeoutException(address, CLIENT_TIMEOUT);
            } else if (S_TIMEOUT == s) {
                throw new JupiterTimeoutException(address, SERVER_TIMEOUT);
            } else if (TIMEOUT == s) {
                if (!sent) {
                    throw new JupiterTimeoutException(address, CLIENT_TIMEOUT);
                } else {
                    throw new JupiterTimeoutException(address, SERVER_TIMEOUT);
                }
            } else {
                throw new JupiterRemoteException(s.name(), address);
            }
        }
    }

    @Override
    protected void notifyListener0(JListener<V> listener, int state, Object x) {
        try {
            if (NORMAL == state) {
                listener.complete((V) x);
            } else {
                Throwable cause = (Throwable) x;
                if (x instanceof Signal) {
                    SocketAddress address = channel.remoteAddress();
                    if (C_TIMEOUT == x) {
                        cause = new JupiterTimeoutException(address, CLIENT_TIMEOUT);
                    } else if (S_TIMEOUT == x) {
                        cause = new JupiterTimeoutException(address, SERVER_TIMEOUT);
                    } else {
                        cause = new JupiterRemoteException(((Signal) x).name(), address);
                    }
                }

                listener.failure(cause);
            }
        } catch (Throwable t) {
            logger.error("An exception was thrown by {}.{}.",
                    listener.getClass().getName(), NORMAL == state ? "complete()" : "failure()", stackTrace(t));
        }
    }

    public void markSent() {
        sent = true;
    }

    public ConsumerHook[] hooks() {
        return hooks;
    }

    public DefaultInvokeFuture<V> hooks(ConsumerHook[] hooks) {
        checkNotNull(hooks, "hooks");

        this.hooks = hooks;
        return this;
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
            setException(new JupiterBizException(response.toString(), channel.remoteAddress()));
        } else {
            setException(new JupiterRemoteException(response.toString(), channel.remoteAddress()));
        }

        // call hook's after method
        for (int i = 0; i < hooks.length; i++) {
            hooks[i].after(response, channel);
        }
    }

    public static void received(JChannel channel, JResponse response) {
        long invokeId = response.id();
        // 在不知道是组播还是单播的情况下需要组播做出性能让步, 查询两次Map
        DefaultInvokeFuture<?> future = roundFutures.remove(invokeId);
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
                    for (DefaultInvokeFuture<?> future : roundFutures.values()) {
                        process(future);
                    }

                    // broadcast
                    for (DefaultInvokeFuture<?> future : broadcastFutures.values()) {
                        process(future);
                    }

                    Thread.sleep(30);
                } catch (Throwable t) {
                    logger.error("An exception has been caught while scanning the timeout futures {}.", t);
                }
            }
        }

        private void process(DefaultInvokeFuture<?> future) {
            if (future == null || future.isDone()) {
                return;
            }

            if (System.nanoTime() - future.startTime > future.timeout) {
                JResponse response = new JResponse(future.invokeId);
                response.status(future.sent ? SERVER_TIMEOUT : CLIENT_TIMEOUT);

                DefaultInvokeFuture.received(future.channel, response);
            }
        }
    }

    static {
        Thread t = new Thread(new TimeoutScanner(), "timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
