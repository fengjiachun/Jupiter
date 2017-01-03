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

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JListener;

import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.Reflects.getTypeDefaultValue;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * 用于实现fail-safe集群容错方案的 {@link InvokeFuture}.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public class FailSafeInvokeFuture<V> implements InvokeFuture<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FailSafeInvokeFuture.class);

    private final InvokeFuture<V> future;

    public static <T> FailSafeInvokeFuture<T> with(InvokeFuture<T> future) {
        return new FailSafeInvokeFuture<>(future);
    }

    private FailSafeInvokeFuture(InvokeFuture<V> future) {
        this.future = future;
    }

    @Override
    public Class<V> returnType() {
        return future.returnType();
    }

    @Override
    public V getResult() throws Throwable {
        try {
            return future.getResult();
        } catch (Throwable t) {
            logger.warn("Ignored exception on [Fail-safe] : {}.", stackTrace(t));
        }
        return (V) getTypeDefaultValue(returnType());
    }

    @Override
    public InvokeFuture<V> addListener(JListener<V> listener) {
        future.addListener(failSafeListener(listener));
        return this;
    }

    @Override
    public InvokeFuture<V> addListeners(JListener<V>... listeners) {
        future.addListeners(failSafeListeners(listeners));
        return this;
    }

    @Override
    public InvokeFuture<V> removeListener(JListener<V> listener) {
        future.removeListener(listener);
        return this;
    }

    @Override
    public InvokeFuture<V> removeListeners(JListener<V>... listeners) {
        future.removeListeners(listeners);
        return this;
    }

    private JListener<V> failSafeListener(JListener<V> listener) {
        return new FailSafeListener<>(listener);
    }

    private JListener<V>[] failSafeListeners(JListener<V>... listeners) {
        checkNotNull(listeners, "listeners");

        JListener<V>[] failSafeListeners = new JListener[listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            failSafeListeners[i] = failSafeListener(listeners[i]);
        }
        return failSafeListeners;
    }

    class FailSafeListener<T> implements JListener<T> {

        private final JListener<T> listener;

        FailSafeListener(JListener<T> listener) {
            this.listener = listener;
        }

        @Override
        public void complete(T result) {
            listener.complete(result);
        }

        @Override
        public void failure(Throwable cause) {
            logger.warn("Ignored exception on [Fail-safe] : {}.", stackTrace(cause));

            listener.complete((T) getTypeDefaultValue(returnType()));
        }
    }
}
