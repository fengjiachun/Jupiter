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

import java.util.concurrent.CompletableFuture;

import org.jupiter.common.util.Requires;

/**
 * 异步调用上下文, 用于获取当前上下文中的 {@link InvokeFuture}, 基于 {@link ThreadLocal}.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public class InvokeFutureContext {

    private static final ThreadLocal<InvokeFuture<?>> futureThreadLocal = new ThreadLocal<>();

    /**
     * 获取单播/广播调用的 {@link InvokeFuture}, 不协助类型转换.
     */
    public static InvokeFuture<?> future() {
        InvokeFuture<?> future = Requires.requireNotNull(futureThreadLocal.get(), "future");
        futureThreadLocal.remove();
        return future;
    }

    /**
     * 获取单播调用的 {@link InvokeFuture} 并协助类型转换, {@code expectReturnType} 为期望定的返回值类型.
     */
    public static <V> InvokeFuture<V> future(Class<V> expectReturnType) {
        InvokeFuture<?> f = future();
        checkReturnType(f.returnType(), expectReturnType);

        return (InvokeFuture<V>) f;
    }

    public static CompletableFuture<?> completableFuture() {
        return (CompletableFuture<?>) future();
    }

    public static <V> CompletableFuture<V>completableFuture(Class<V> expectReturnType) {
        return (CompletableFuture<V>) future(expectReturnType);
    }

    /**
     * 获取广播调用的 {@link InvokeFutureGroup} 并协助类型转换, {@code expectReturnType} 为期望定的返回值类型.
     */
    public static <V> InvokeFutureGroup<V> futureBroadcast(Class<V> expectReturnType) {
        InvokeFuture<?> f = future();
        checkReturnType(f.returnType(), expectReturnType);

        if (f instanceof InvokeFutureGroup) {
            return (InvokeFutureGroup<V>) f;
        } else if (f instanceof FailsafeInvokeFuture) {
            InvokeFuture real_f = ((FailsafeInvokeFuture) f).future();
            if (real_f instanceof InvokeFutureGroup) {
                return (InvokeFutureGroup<V>) real_f;
            }
        }
        throw new UnsupportedOperationException("broadcast");
    }

    public static void set(InvokeFuture<?> future) {
        futureThreadLocal.set(future);
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
