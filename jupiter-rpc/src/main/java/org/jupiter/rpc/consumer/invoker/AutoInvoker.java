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

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

/**
 *
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class AutoInvoker extends AbstractInvoker {

    public AutoInvoker(String appName,
                       ServiceMetadata metadata,
                       Dispatcher dispatcher,
                       ClusterStrategyConfig defaultStrategy,
                       List<MethodSpecialConfig> methodSpecialConfigs) {
        super(appName, metadata, dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    @SuppressWarnings("unchecked")
    @RuntimeType
    public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
        Class<?> returnType = method.getReturnType();

        if (!CompletableFuture.class.isAssignableFrom(returnType)) {
            return doInvoke(method.getName(), args, returnType, true);
        }

        final CompletableFuture<Object> cf = createCompletableFuture((Class<CompletableFuture>) returnType);

        InvokeFuture<Object> inf = (InvokeFuture<Object>) doInvoke(method.getName(), args, returnType, false);
        inf.whenComplete((result, throwable) -> {
            if (throwable == null) {
                try {
                    cf.complete(result);
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                }
            } else {
                cf.completeExceptionally(throwable);
            }
        });

        return cf;
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Object> createCompletableFuture(Class<CompletableFuture> cls) {
        if (cls == CompletableFuture.class) {
            return new CompletableFuture<>();
        }
        try {
            return cls.newInstance();
        } catch (Throwable t) {
            throw new UnsupportedOperationException("fail to create instance with default constructor", t);
        }
    }
}
