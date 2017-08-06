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

package org.jupiter.rpc.consumer.invoker;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Synchronous call.
 *
 * 同步调用.
 *
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class SyncInvoker extends ClusterStrategyBridging {

    public SyncInvoker(JClient client,
                       Dispatcher dispatcher,
                       ClusterStrategyConfig defaultStrategy,
                       List<MethodSpecialConfig> methodSpecialConfigs) {

        super(client, dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    @RuntimeType
    public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
        String methodName = method.getName();
        ClusterInvoker invoker = getClusterInvoker(methodName);
        InvokeFuture<?> future = invoker.invoke(methodName, args, method.getReturnType());
        return future.getResult();
    }
}
