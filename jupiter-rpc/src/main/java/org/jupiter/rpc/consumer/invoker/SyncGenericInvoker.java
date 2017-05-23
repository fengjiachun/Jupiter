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

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;

import java.util.List;

/**
 * 同步泛化调用.
 *
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @see GenericInvoker
 *
 * @author jiachun.fjc
 */
public class SyncGenericInvoker extends ClusterBridging implements GenericInvoker {

    public SyncGenericInvoker(JClient client,
                              Dispatcher dispatcher,
                              ClusterStrategyConfig defaultStrategy,
                              List<MethodSpecialConfig> methodSpecialConfigs) {

        super(client, dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    @Override
    public Object $invoke(String methodName, Object... args) throws Throwable {
        ClusterInvoker invoker = getClusterInvoker(methodName);
        InvokeFuture<?> future = invoker.invoke(methodName, args, Object.class);
        return future.getResult();
    }
}
