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
import org.jupiter.common.util.Reflects;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;

import java.lang.reflect.Method;

/**
 * Asynchronous call, {@link CallbackInvoker#invoke(Method, Object[])}
 * returns a default value of the corresponding method.
 *
 * 异步回调
 *
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class CallbackInvoker {

    private final ClusterInvoker clusterInvoker;

    public CallbackInvoker(ClusterInvoker clusterInvoker) {
        this.clusterInvoker = clusterInvoker;
    }

    @RuntimeType
    public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
        Class<?> returnType = method.getReturnType();
        Object future = clusterInvoker.invoke(method.getName(), args, returnType);
        InvokeFutureContext.set(future);
        return Reflects.getTypeDefaultValue(returnType);
    }
}
