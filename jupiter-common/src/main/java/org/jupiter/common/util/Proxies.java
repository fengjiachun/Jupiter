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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public enum Proxies {
    JDK_PROXY(new ProxyDelegate() {

        @Override
        public <T> T newProxy(Class<T> interfaceType, Object handler) {
            Requires.requireTrue(handler instanceof InvocationHandler, "handler must be a InvocationHandler");

            Object object = Proxy.newProxyInstance(
                    interfaceType.getClassLoader(), new Class<?>[] { interfaceType }, (InvocationHandler) handler);

            return interfaceType.cast(object);
        }
    }),
    BYTE_BUDDY(new ProxyDelegate() {

        @Override
        public <T> T newProxy(Class<T> interfaceType, Object handler) {
            Class<? extends T> cls = new ByteBuddy()
                    .subclass(interfaceType)
                    .method(ElementMatchers.isDeclaredBy(interfaceType))
                    .intercept(MethodDelegation.to(handler, "handler"))
                    .make()
                    .load(interfaceType.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();

            try {
                return cls.newInstance();
            } catch (Throwable t) {
                ThrowUtil.throwException(t);
            }
            return null; // never get here
        }
    });

    private final ProxyDelegate delegate;

    Proxies(ProxyDelegate delegate) {
        this.delegate = delegate;
    }

    public static Proxies getDefault() {
        return BYTE_BUDDY;
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
