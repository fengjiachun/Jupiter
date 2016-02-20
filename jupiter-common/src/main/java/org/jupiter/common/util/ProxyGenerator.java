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

package org.jupiter.common.util;

import net.bytebuddy.ByteBuddy;
import org.jupiter.common.util.internal.JUnsafe;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.jupiter.common.util.Preconditions.checkArgument;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public enum ProxyGenerator {
    JDK_PROXY(new ProxyDelegate() {

        @Override
        public <T> T newProxy(Class<T> interfaceType, Object handler) {
            checkArgument(handler instanceof InvocationHandler, "handler must be a InvocationHandler");

            Object object = Proxy.newProxyInstance(
                    interfaceType.getClassLoader(), new Class<?>[] { interfaceType }, (InvocationHandler) handler);

            return interfaceType.cast(object);
        }
    }),
    BYTE_BUDDY(new ProxyDelegate() {

        @Override
        public <T> T newProxy(Class<T> interfaceType, Object handler) {
            try {
                return new ByteBuddy()
                        .subclass(interfaceType)
                        .method(isDeclaredBy(interfaceType))
                        .intercept(to(handler, "handler").filter(not(isDeclaredBy(Object.class))))
                        .make()
                        .load(interfaceType.getClassLoader(), INJECTION)
                        .getLoaded()
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                JUnsafe.throwException(e);
            }

            // should never get here
            return null;
        }
    });

    private final ProxyDelegate delegate;

    ProxyGenerator(ProxyDelegate delegate) {
        this.delegate = delegate;
    }

    public <T> T newProxy(Class<T> interfaceType, Object handler) {
        return delegate.newProxy(interfaceType, handler);
    }

    interface ProxyDelegate {

        /**
         * Returns a proxy instance that implements {@code interfaceType} by dispatching
         * method invocations to {@code handler}. The class loader of {@code interfaceType}
         * will be used to define the proxy class.
         */
        <T> T newProxy(Class<T> interfaceType, Object handler);
    }
}
