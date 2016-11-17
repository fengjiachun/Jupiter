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

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public class InvokeFutureContext {

    private static final ThreadLocal<InvokeFuture<?>> futureThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<InvokeFuture<?>[]> futureGroupThreadLocal = new ThreadLocal<>();

    public static InvokeFuture<?> future() {
        InvokeFuture<?> future = checkNotNull(futureThreadLocal.get(), "future");
        futureThreadLocal.remove();
        return future;
    }

    public static InvokeFuture<?>[] futures() {
        InvokeFuture<?>[] futures = checkNotNull(futureGroupThreadLocal.get(), "futures");
        futureGroupThreadLocal.remove();
        return futures;
    }

    @SuppressWarnings("unchecked")
    public static <V> InvokeFuture<V> future(Class<V> expectReturnType) {
        InvokeFuture<?> f = future();
        Class<?> realReturnType = f.getReturnType();
        checkReturnType(realReturnType, expectReturnType);
        return (InvokeFuture<V>) f;
    }

    @SuppressWarnings("unchecked")
    public static <V> InvokeFuture<V>[] futures(Class<V> expectReturnType) {
        InvokeFuture<?>[] futures = futures();
        InvokeFuture<V>[] v_futures = new InvokeFuture[futures.length];
        for (int i = 0; i < futures.length; i++) {
            InvokeFuture<?> f = futures[i];
            Class<?> realReturnType = f.getReturnType();
            checkReturnType(realReturnType, expectReturnType);
            v_futures[i] = (InvokeFuture<V>) f;
        }
        return v_futures;
    }

    public static void set(Object obj) {
        if (obj instanceof InvokeFuture<?>) {
            futureThreadLocal.set((InvokeFuture<?>) obj);
        } else if (obj instanceof InvokeFuture<?>[]) {
            futureGroupThreadLocal.set((InvokeFuture<?>[]) obj);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static void checkReturnType(Class<?> realType, Class<?> expectType) {
        if (!expectType.isAssignableFrom(realType)) {
            throw new IllegalArgumentException(
                    "illegal returnType, expect type is ["
                            + expectType.getName()
                            + "], but real type is ["
                            + realType.getName() + "]");
        }
    }
}
