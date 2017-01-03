/*
 * Copyright (c) 2016 The Jupiter Project
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

package org.jupiter.rpc.consumer.invoker;

import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;

/**
 * 异步泛化调用.
 *
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @see GenericInvoker
 *
 * @author jiachun.fjc
 */
public class AsyncGenericInvoker implements GenericInvoker {

    private final ClusterInvoker clusterInvoker;

    public AsyncGenericInvoker(ClusterInvoker clusterInvoker) {
        this.clusterInvoker = clusterInvoker;
    }

    @Override
    public Object $invoke(String methodName, Object... args) throws Throwable {
        InvokeFuture<?> future = clusterInvoker.invoke(methodName, args, Object.class);
        InvokeFutureContext.set(future);
        return null;
    }
}
