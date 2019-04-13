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

import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.consumer.cluster.FailsafeClusterInvoker;

/**
 * 用于实现fail-safe集群容错方案的 {@link InvokeFuture}.
 *
 * 同步调用时发生异常时只打印日志.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @see FailsafeClusterInvoker
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public class FailsafeInvokeFuture<V> extends CompletableFuture<V> implements InvokeFuture<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FailsafeInvokeFuture.class);

    private final InvokeFuture<V> future;

    public static <T> FailsafeInvokeFuture<T> with(InvokeFuture<T> future) {
        return new FailsafeInvokeFuture<>(future);
    }

    private FailsafeInvokeFuture(InvokeFuture<V> future) {
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
            if (logger.isWarnEnabled()) {
                logger.warn("Ignored exception on [Fail-safe]: {}.", StackTraceUtil.stackTrace(t));
            }
        }
        return (V) Reflects.getTypeDefaultValue(returnType());
    }

    public InvokeFuture<V> future() {
        return future;
    }
}
