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

import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Signal;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.consumer.ConsumerInterceptor;
import org.jupiter.rpc.exception.JupiterBizException;
import org.jupiter.rpc.exception.JupiterRemoteException;
import org.jupiter.rpc.exception.JupiterSerializationException;
import org.jupiter.rpc.exception.JupiterTimeoutException;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.transport.Status;
import org.jupiter.transport.channel.JChannel;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public class DefaultInvokeFuture<V> extends AbstractListenableFuture<V> implements InvokeFuture<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultInvokeFuture.class);

    private static final long DEFAULT_TIMEOUT_NANOSECONDS = TimeUnit.MILLISECONDS.toNanos(JConstants.DEFAULT_TIMEOUT);

    private static final ConcurrentMap<Long, DefaultInvokeFuture<?>> roundFutures = Maps.newConcurrentMapLong();
    private static final ConcurrentMap<String, DefaultInvokeFuture<?>> broadcastFutures = Maps.newConcurrentMap();

    private final long invokeId; // request.invokeId, 广播的场景可以重复
    private final JChannel channel;
    private final Class<V> returnType;
    private final long timeout;
    private final long startTime = System.nanoTime();

    private volatile boolean sent = false;

    private ConsumerInterceptor[] interceptors;
    private TraceId traceId;

    public static <T> DefaultInvokeFuture<T> with(
            long invokeId, JChannel channel, long timeoutMillis, Class<T> returnType, DispatchType dispatchType) {

        return new DefaultInvokeFuture<>(invokeId, channel, timeoutMillis, returnType, dispatchType);
    }

    private DefaultInvokeFuture(
            long invokeId, JChannel channel, long timeoutMillis, Class<V> returnType, DispatchType dispatchType) {

        this.invokeId = invokeId;
        this.channel = channel;
        this.timeout = timeoutMillis > 0 ? TimeUnit.MILLISECONDS.toNanos(timeoutMillis) : DEFAULT_TIMEOUT_NANOSECONDS;
        this.returnType = returnType;

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

    public JChannel channel() {
        return channel;
    }

    @Override
    public Class<V> returnType() {
        return returnType;
    }

    @Override
    public V getResult() throws Throwable {
        try {
            return get(timeout, TimeUnit.NANOSECONDS);
        } catch (Signal s) {
            SocketAddress address = channel.remoteAddress();
            if (s == TIMEOUT) {
                throw new JupiterTimeoutException(address, sent ? Status.SERVER_TIMEOUT : Status.CLIENT_TIMEOUT);
            } else {
                throw new JupiterRemoteException(s.name(), address);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void notifyListener0(JListener<V> listener, int state, Object x) {
        try {
            if (state == NORMAL) {
                listener.complete((V) x);
            } else {
                listener.failure((Throwable) x);
            }
        } catch (Throwable t) {
            logger.error("An exception was thrown by {}.{}, {}.",
                    listener.getClass().getName(), state == NORMAL ? "complete()" : "failure()", stackTrace(t));
        }
    }

    public void markSent() {
        sent = true;
    }

    public ConsumerInterceptor[] interceptors() {
        return interceptors;
    }

    public DefaultInvokeFuture<V> interceptors(ConsumerInterceptor[] interceptors) {
        this.interceptors = interceptors;
        return this;
    }

    public TraceId traceId() {
        return traceId;
    }

    public DefaultInvokeFuture<V> traceId(TraceId traceId) {
        this.traceId = traceId;
        return this;
    }

    @SuppressWarnings("all")
    private void doReceived(JResponse response) {
        byte status = response.status();

        if (status == Status.OK.value()) {
            ResultWrapper wrapper = response.result();
            set((V) wrapper.getResult());
        } else {
            setException(status, response);
        }

        ConsumerInterceptor[] interceptors = this.interceptors; // snapshot
        if (interceptors != null) {
            for (int i = interceptors.length - 1; i >= 0; i--) {
                interceptors[i].afterInvoke(traceId, response, channel);
            }
        }
    }

    private void setException(byte status, JResponse response) {
        Throwable cause;
        if (status == Status.SERVER_TIMEOUT.value()) {
            cause = new JupiterTimeoutException(channel.remoteAddress(), Status.SERVER_TIMEOUT);
        } else if (status == Status.CLIENT_TIMEOUT.value()) {
            cause = new JupiterTimeoutException(channel.remoteAddress(), Status.CLIENT_TIMEOUT);
        } else if (status == Status.DESERIALIZATION_FAIL.value()) {
            ResultWrapper wrapper = response.result();
            cause = (JupiterSerializationException) wrapper.getResult();
        } else if (status == Status.SERVICE_EXPECTED_ERROR.value()) {
            ResultWrapper wrapper = response.result();
            cause = (Throwable) wrapper.getResult();
        } else if (status == Status.SERVICE_UNEXPECTED_ERROR.value()) {
            ResultWrapper wrapper = response.result();
            String message = String.valueOf(wrapper.getResult());
            cause = new JupiterBizException(message, channel.remoteAddress());
        } else {
            ResultWrapper wrapper = response.result();
            Object result = wrapper.getResult();
            if (result != null && result instanceof JupiterRemoteException) {
                cause = (JupiterRemoteException) result;
            } else {
                cause = new JupiterRemoteException(response.toString(), channel.remoteAddress());
            }
        }
        setException(cause);
    }

    public static void received(JChannel channel, JResponse response) {
        long invokeId = response.id();

        DefaultInvokeFuture<?> future = roundFutures.remove(invokeId);

        if (future == null) {
            // 广播场景下做出了一点让步, 多查询了一次roundFutures
            future = broadcastFutures.remove(subInvokeId(channel, invokeId));
        }

        if (future == null) {
            logger.warn("A timeout response [{}] finally returned on {}.", response, channel);
            return;
        }

        future.doReceived(response);
    }

    public static void fakeReceived(JChannel channel, JResponse response, DispatchType dispatchType) {
        long invokeId = response.id();

        DefaultInvokeFuture<?> future = null;

        if (dispatchType == DispatchType.ROUND) {
            future = roundFutures.remove(invokeId);
        } else if (dispatchType == DispatchType.BROADCAST) {
            future = broadcastFutures.remove(subInvokeId(channel, invokeId));
        }

        if (future == null) {
            return; // 正确结果在超时被处理之前返回
        }

        future.doReceived(response);
    }

    private static String subInvokeId(JChannel channel, long invokeId) {
        return channel.id() + invokeId;
    }

    // timeout scanner
    @SuppressWarnings("all")
    private static class TimeoutScanner implements Runnable {

        private static final long TIMEOUT_SCANNER_INTERVAL_MILLIS =
                SystemPropertyUtil.getLong("jupiter.rpc.invoke.timeout_scanner_interval_millis", 100);

        public void run() {
            for (;;) {
                try {
                    // round
                    for (DefaultInvokeFuture<?> future : roundFutures.values()) {
                        process(future, DispatchType.ROUND);
                    }

                    // broadcast
                    for (DefaultInvokeFuture<?> future : broadcastFutures.values()) {
                        process(future, DispatchType.BROADCAST);
                    }
                } catch (Throwable t) {
                    logger.error("An exception was caught while scanning the timeout futures {}.", stackTrace(t));
                }

                try {
                    Thread.sleep(TIMEOUT_SCANNER_INTERVAL_MILLIS);
                } catch (InterruptedException ignored) {}
            }
        }

        private void process(DefaultInvokeFuture<?> future, DispatchType dispatchType) {
            if (future == null || future.isDone()) {
                return;
            }

            if (System.nanoTime() - future.startTime > future.timeout) {
                JResponse response = new JResponse(future.invokeId);
                response.status(future.sent ? Status.SERVER_TIMEOUT : Status.CLIENT_TIMEOUT);

                DefaultInvokeFuture.fakeReceived(future.channel, response, dispatchType);
            }
        }
    }

    static {
        Thread t = new Thread(new TimeoutScanner(), "timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
