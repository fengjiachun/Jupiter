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
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public class FailSafeInvokeFuture<V> implements InvokeFuture<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FailSafeInvokeFuture.class);

    private final String name;
    private final InvokeFuture<V> future;

    public FailSafeInvokeFuture(String name, InvokeFuture<V> future) {
        this.name = name;
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
            logger.warn("Ignored exception on [{}] : {}.", name, stackTrace(t));
        }
        return null;
    }

    @Override
    public ListenableFuture<V> addListener(JListener<V> listener) {
        return future.addListener(failSafeListener(listener));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListenableFuture<V> addListeners(JListener<V>... listeners) {
        return future.addListeners(failSafeListeners(listeners));
    }

    @Override
    public ListenableFuture<V> removeListener(JListener<V> listener) {
        return future.removeListener(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListenableFuture<V> removeListeners(JListener<V>... listeners) {
        return future.removeListeners(listeners);
    }

    private JListener<V> failSafeListener(JListener<V> listener) {
        return new FailSafeListener<>(listener);
    }

    @SuppressWarnings("unchecked")
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
            logger.warn("Ignored exception on [{}] : {}.", name, stackTrace(cause));

            listener.complete(null);
        }
    }
}
