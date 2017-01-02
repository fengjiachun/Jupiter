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

import org.jupiter.rpc.JListener;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public class DefaultInvokeFutureGroup<V> implements InvokeFuture<V>, InvokeFutureGroup<V> {

    private final InvokeFuture<V>[] futures;

    @SuppressWarnings("unchecked")
    public static DefaultInvokeFutureGroup<?> with(InvokeFuture<?>[] futures) {
        return new DefaultInvokeFutureGroup(futures);
    }

    private DefaultInvokeFutureGroup(InvokeFuture<V>[] futures) {
        this.futures = futures;
    }

    @Override
    public Class<V> returnType() {
        return futures[0].returnType();
    }

    @Override
    public V getResult() throws Throwable {
        throw new UnsupportedOperationException("broadcast");
    }

    @Override
    public InvokeFuture<V>[] futures() {
        return futures;
    }

    @Override
    public InvokeFuture<V> addListener(JListener<V> listener) {
        for (InvokeFuture<V> f : futures) {
            f.addListener(listener);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public InvokeFuture<V> addListeners(JListener<V>... listeners) {
        for (InvokeFuture<V> f : futures) {
            f.addListeners(listeners);
        }
        return this;
    }

    @Override
    public InvokeFuture<V> removeListener(JListener<V> listener) {
        for (InvokeFuture<V> f : futures) {
            f.removeListener(listener);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public InvokeFuture<V> removeListeners(JListener<V>... listeners) {
        for (InvokeFuture<V> f : futures) {
            f.removeListeners(listeners);
        }
        return this;
    }
}
