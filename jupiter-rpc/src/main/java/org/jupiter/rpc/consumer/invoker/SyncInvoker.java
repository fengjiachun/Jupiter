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

import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 同步调用
 *
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class SyncInvoker implements InvocationHandler {

    private final Dispatcher dispatcher;
    private final MessageWrapper message;

    public SyncInvoker(Dispatcher dispatcher, MessageWrapper message) {
        this.dispatcher = dispatcher;
        this.message = message;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        message.setMethodName(method.getName());
        message.setParameterTypes(method.getParameterTypes());
        message.setArgs(args);

        InvokeFuture result = dispatcher.dispatch(message);

        return result.singleResult();
    }
}
