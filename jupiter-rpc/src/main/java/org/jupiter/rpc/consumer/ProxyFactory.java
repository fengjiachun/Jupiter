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

package org.jupiter.rpc.consumer;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.Strings;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.CallbackInvoker;
import org.jupiter.rpc.consumer.invoker.PromiseInvoker;
import org.jupiter.rpc.consumer.invoker.SyncInvoker;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.Reflects.ProxyGeneratorOption.*;
import static org.jupiter.rpc.DispatchMode.BROADCAST;
import static org.jupiter.rpc.DispatchMode.ROUND;
import static org.jupiter.rpc.InvokeMode.CALLBACK;
import static org.jupiter.rpc.InvokeMode.SYNC;

/**
 * Proxy factory
 *
 * jupiter
 * org.jupiter.rpc.consumer
 *
 * @author jiachun.fjc
 */
public class ProxyFactory<I> {

    private final Class<I> interfaceClass;

    private JClient client;
    private List<UnresolvedAddress> addresses;
    private InvokeMode invokeMode = SYNC;
    private DispatchMode dispatchMode = ROUND;
    private int timeoutMillis;
    private Map<String, Integer> methodsSpecialTimeoutMillis;
    private JListener listener;
    private List<ConsumerHook> hooks;

    public static <I> ProxyFactory<I> factory(Class<I> interfaceClass) {
        ProxyFactory<I> factory = new ProxyFactory<>(interfaceClass);
        // 初始化数据
        factory.addresses = Lists.newArrayList();
        factory.hooks = Lists.newArrayList();
        factory.methodsSpecialTimeoutMillis = Maps.newTreeMap();

        return factory;
    }

    private ProxyFactory(Class<I> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Class<I> getInterfaceClass() {
        return interfaceClass;
    }

    /**
     * Sets the connector.
     */
    public ProxyFactory<I> connector(JClient client) {
        this.client = client;
        return this;
    }

    /**
     * Adds provider's addresses.
     */
    public ProxyFactory<I> addProviderAddress(UnresolvedAddress... addresses) {
        Collections.addAll(this.addresses, addresses);
        return this;
    }

    /**
     * Adds provider's addresses.
     */
    public ProxyFactory<I> addProviderAddress(List<UnresolvedAddress> addresses) {
        this.addresses.addAll(addresses);
        return this;
    }

    /**
     * Synchronous blocking, asynchronous with future or asynchronous with callback,
     * the default is synchronous.
     */
    public ProxyFactory<I> invokeMode(InvokeMode invokeMode) {
        this.invokeMode = checkNotNull(invokeMode);
        return this;
    }

    /**
     * Sets the mode of dispatch, the default is {@link DispatchMode#ROUND}
     */
    public ProxyFactory<I> dispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = checkNotNull(dispatchMode);
        return this;
    }

    /**
     * Timeout milliseconds.
     */
    public ProxyFactory<I> timeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    /**
     * Method special timeout milliseconds.
     */
    public ProxyFactory<I> methodSpecialTimeoutMillis(String methodName, int timeoutMillis) {
        methodsSpecialTimeoutMillis.put(methodName, timeoutMillis);
        return this;
    }

    /**
     * Asynchronous callback listener.
     */
    public ProxyFactory<I> listener(JListener listener) {
        if (invokeMode != CALLBACK) {
            throw new UnsupportedOperationException("invokeMode should first be set to CALLBACK");
        }
        this.listener = listener;
        return this;
    }

    /**
     * Adds hooks.
     */
    public ProxyFactory<I> addHook(ConsumerHook... hooks) {
        Collections.addAll(this.hooks, hooks);
        return this;
    }

    public I newProxyInstance() {
        // check arguments
        checkNotNull(client, "connector");
        checkNotNull(interfaceClass, "interfaceClass");
        if (dispatchMode == BROADCAST && invokeMode != CALLBACK) {
            throw new UnsupportedOperationException("illegal mode, BROADCAST only support CALLBACK");
        }
        ServiceProvider annotation = interfaceClass.getAnnotation(ServiceProvider.class);
        checkNotNull(annotation, interfaceClass + " is not a ServiceProvider interface");
        String providerName = annotation.value();
        providerName = Strings.isNotBlank(providerName) ? providerName : interfaceClass.getSimpleName();

        // metadata
        ServiceMetadata metadata = new ServiceMetadata(annotation.group(), annotation.version(), providerName);

        for (UnresolvedAddress address : addresses) {
            client.addChannelGroup(metadata, client.group(address));
        }

        // dispatcher
        Dispatcher dispatcher = asDispatcher(metadata);
        if (timeoutMillis > 0) {
            dispatcher.setTimeoutMillis(timeoutMillis);
        }
        if (!methodsSpecialTimeoutMillis.isEmpty()) {
            dispatcher.setMethodsSpecialTimeoutMillis(methodsSpecialTimeoutMillis);
        }
        dispatcher.setHooks(hooks);

        Object handler;
        switch (invokeMode) {
            case SYNC:
                handler = new SyncInvoker(client, dispatcher);
                break;
            case PROMISE:
                handler = new PromiseInvoker(client, dispatcher);
                break;
            case CALLBACK:
                dispatcher.setListener(checkNotNull(listener, "listener"));
                handler = new CallbackInvoker(client, dispatcher);
                break;
            default:
                throw new IllegalStateException("InvokeMode: " + invokeMode);
        }

        return Reflects.newProxy(interfaceClass, handler, BYTE_BUDDY);
    }

    protected Dispatcher asDispatcher(ServiceMetadata metadata) {
        switch (dispatchMode) {
            case ROUND:
                return new DefaultRoundDispatcher(metadata);
            case BROADCAST:
                return new DefaultBroadcastDispatcher(metadata);
            default:
                throw new IllegalStateException("DispatchMode: " + dispatchMode);
        }
    }
}
