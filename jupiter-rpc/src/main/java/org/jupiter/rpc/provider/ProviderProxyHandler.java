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

package org.jupiter.rpc.provider;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.jupiter.common.concurrent.atomic.AtomicUpdater;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingEye;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.rpc.provider
 *
 * @author jiachun.fjc
 */
public class ProviderProxyHandler {

    private static final AtomicReferenceFieldUpdater<CopyOnWriteArrayList, Object[]> interceptorsUpdater =
            AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteArrayList.class, Object[].class, "array");

    private final CopyOnWriteArrayList<ProviderInterceptor> interceptors = new CopyOnWriteArrayList<>();

    @SuppressWarnings("all")
    @RuntimeType
    public Object invoke(
            @SuperCall Callable<?> superMethod,
            @Origin Method method,
            @AllArguments @RuntimeType Object[] args) throws Throwable {
        TraceId traceId = TracingEye.getCurrent();
        String methodName = method.getName();
        // snapshot, 保证before和after使用相同版本的interceptors
        Object[] elements = interceptorsUpdater.get(interceptors);

        for (int i = elements.length - 1; i >= 0; i--) {
            ((ProviderInterceptor) elements[i]).before(traceId, methodName, args);
        }
        Object result = null;
        try {
            result = superMethod.call();
        } finally {
            for (int i = 0; i < elements.length; i++) {
                ((ProviderInterceptor) elements[i]).after(traceId, methodName, args, result);
            }
        }
        return result;
    }

    public ProviderProxyHandler addProviderInterceptor(ProviderInterceptor interceptor) {
        interceptors.add(checkNotNull(interceptor, "interceptor"));
        return this;
    }
}
