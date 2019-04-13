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

import org.jupiter.rpc.consumer.cluster.FailsafeClusterInvoker;

/**
 * 用于实现failover集群容错方案的 {@link InvokeFuture}.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @see FailsafeClusterInvoker
 *
 * @author jiachun.fjc
 */
public class FailoverInvokeFuture<V> extends CompletableFuture<V> implements InvokeFuture<V> {

    private final Class<V> returnType;

    public static <T> FailoverInvokeFuture<T> with(Class<T> returnType) {
        return new FailoverInvokeFuture<>(returnType);
    }

    private FailoverInvokeFuture(Class<V> returnType) {
        this.returnType = returnType;
    }

    @Override
    public Class<V> returnType() {
        return returnType;
    }

    @Override
    public V getResult() throws Throwable {
        return get();
    }
}
