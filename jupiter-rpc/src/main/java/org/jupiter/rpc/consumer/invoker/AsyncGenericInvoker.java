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

import java.util.List;

import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

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
public class AsyncGenericInvoker extends AbstractInvoker implements GenericInvoker {

    public AsyncGenericInvoker(String appName,
                               ServiceMetadata metadata,
                               Dispatcher dispatcher,
                               ClusterStrategyConfig defaultStrategy,
                               List<MethodSpecialConfig> methodSpecialConfigs) {
        super(appName, metadata, dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    @Override
    public Object $invoke(String methodName, Object... args) throws Throwable {
        Object result = doInvoke(methodName, args, Object.class, false);

        InvokeFutureContext.set((InvokeFuture<?>) result);

        return null;
    }
}
