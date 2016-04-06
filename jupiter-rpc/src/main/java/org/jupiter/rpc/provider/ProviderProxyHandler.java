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
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingEye;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.provider
 *
 * @author jiachun.fjc
 */
public class ProviderProxyHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProviderProxyHandler.class);

    private static final AtomicReferenceFieldUpdater<CopyOnWriteArrayList, Object[]> interceptorsUpdater =
            AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteArrayList.class, Object[].class, "array");

    private final CopyOnWriteArrayList<ProviderInterceptor> interceptors = new CopyOnWriteArrayList<>();

    @SuppressWarnings("all")
    @RuntimeType
    public Object invoke(
            @SuperCall Callable<Object> superMethod,
            @Origin Method method,
            @AllArguments @RuntimeType Object[] args) throws Throwable {
        TraceId traceId = TracingEye.getCurrent();
        String methodName = method.getName();
        // snapshot, 保证before和after使用相同版本的interceptors
        Object[] elements = interceptorsUpdater.get(interceptors);

        for (int i = elements.length - 1; i >= 0; i--) {
            ProviderInterceptor interceptor = (ProviderInterceptor) elements[i];
            try {
                interceptor.beforeInvoke(traceId, methodName, args);
            } catch (Throwable t) {
                logger.warn("Interceptor[{}#beforeInvoke]: {}.", interceptor.getClass().getName(), stackTrace(t));
            }
        }
        Object result = null;
        try {
            result = superMethod.call();
        } finally {
            for (int i = 0; i < elements.length; i++) {
                ProviderInterceptor interceptor = (ProviderInterceptor) elements[i];
                try {
                    interceptor.afterInvoke(traceId, methodName, args, result);
                } catch (Throwable t) {
                    logger.warn("Interceptor[{}#afterInvoke]: {}.", interceptor.getClass().getName(), stackTrace(t));
                }
            }
        }
        return result;
    }

    public ProviderProxyHandler withIntercept(ProviderInterceptor interceptor) {
        interceptors.add(checkNotNull(interceptor, "interceptor"));
        return this;
    }

    public ProviderProxyHandler withIntercept(ProviderInterceptor... interceptors) {
        for (ProviderInterceptor interceptor : interceptors) {
            withIntercept(interceptor);
        }
        return this;
    }
}
