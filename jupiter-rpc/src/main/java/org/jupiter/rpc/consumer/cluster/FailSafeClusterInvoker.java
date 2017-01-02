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

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.FailSafeInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Reflects.simpleClassName;

/**
 * 失败安全, 出现异常时, 直接忽略.
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
public class FailSafeClusterInvoker extends AbstractClusterInvoker {

    public FailSafeClusterInvoker(JClient client, Dispatcher dispatcher) {
        super(client, dispatcher);

        checkArgument(
                dispatcher instanceof DefaultRoundDispatcher,
                simpleClassName(dispatcher) + " is unsupported [FailSafeClusterInvoker]"
        );
    }

    @Override
    public String name() {
        return "Fail-safe";
    }

    @Override
    public InvokeFuture<?> invoke(String methodName, Object[] args, Class<?> returnType) throws Exception {
        InvokeFuture<?> future = dispatcher.dispatch(client, methodName, args, returnType);
        return new FailSafeInvokeFuture<>(future);
    }
}
