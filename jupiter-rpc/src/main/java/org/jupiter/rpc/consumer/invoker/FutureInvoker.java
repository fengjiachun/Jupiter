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

import org.jupiter.common.util.Reflects;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.JFuture;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Asynchronous call, {@link FutureInvoker#invoke(Object, Method, Object[])}
 * returns a default value of the corresponding method.
 *
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class FutureInvoker implements InvocationHandler {

    private static final ThreadLocal<JFuture> futureThreadLocal = new ThreadLocal<>();

    private final JClient client;
    private final Dispatcher dispatcher;

    public FutureInvoker(JClient client, Dispatcher dispatcher) {
        this.client = client;
        this.dispatcher = dispatcher;
    }

    public static JFuture future() {
        JFuture future = futureThreadLocal.get();
        if (future == null) {
            throw new UnsupportedOperationException("future");
        }
        futureThreadLocal.remove();
        return future;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        JFuture future = dispatcher.dispatch(client, method.getName(), args);
        futureThreadLocal.set(future);
        return Reflects.getTypeDefaultValue(method.getReturnType());
    }
}
