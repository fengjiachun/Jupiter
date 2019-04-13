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
package org.jupiter.rpc.consumer.cluster;

import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.FailsafeInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;

/**
 * 失败安全, 同步调用时发生异常时只打印日志.
 *
 * 通常用于写入审计日志等操作.
 *
 * http://en.wikipedia.org/wiki/Fail-safe
 *
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailsafeClusterInvoker implements ClusterInvoker {

    private final Dispatcher dispatcher;

    public FailsafeClusterInvoker(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public Strategy strategy() {
        return Strategy.FAIL_SAFE;
    }

    @Override
    public <T> InvokeFuture<T> invoke(JRequest request, Class<T> returnType) throws Exception {
        InvokeFuture<T> future = dispatcher.dispatch(request, returnType);
        return FailsafeInvokeFuture.with(future);
    }
}
