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
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.Strings;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.AsyncInvoker;
import org.jupiter.rpc.consumer.invoker.SyncInvoker;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import java.util.Collections;
import java.util.List;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.rpc.AsyncMode.ASYNC_CALLBACK;
import static org.jupiter.rpc.AsyncMode.SYNC;
import static org.jupiter.rpc.DispatchMode.BROADCAST;
import static org.jupiter.rpc.DispatchMode.ROUND;

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
    private AsyncMode asyncMode = SYNC;
    private DispatchMode dispatchMode = ROUND;
    private int timeoutMills;
    private JListener listener;
    private List<ConsumerHook> hooks;

    public static <I> ProxyFactory<I> factory(Class<I> interfaceClass) {
        ProxyFactory<I> factory = new ProxyFactory<>(interfaceClass);
        ConsumerHook tracingHook = new TraceLoggingHook();
        // 初始化数据
        factory.addresses = Lists.newArrayList();
        factory.hooks = Lists.newArrayList(tracingHook);

        return factory;
    }

    private ProxyFactory(Class<I> interfaceClass) {
        this.interfaceClass = interfaceClass;
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
     * Synchronous blocking or asynchronous callback, the default is synchronous.
     */
    public ProxyFactory<I> asyncMode(AsyncMode asyncMode) {
        this.asyncMode = checkNotNull(asyncMode);
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
    public ProxyFactory<I> timeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
        return this;
    }

    /**
     * Asynchronous callback listener.
     */
    public ProxyFactory<I> listener(JListener listener) {
        if (asyncMode != ASYNC_CALLBACK) {
            throw new UnsupportedOperationException("asyncMode should first be set to ASYNC_CALLBACK");
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
        checkArgument(!(asyncMode == SYNC && dispatchMode == BROADCAST), "illegal mode, [SYNC & BROADCAST] unsupported");
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
        Dispatcher dispatcher = asDispatcher(dispatchMode, metadata);
        if (timeoutMills > 0) {
            dispatcher.setTimeoutMills(timeoutMills);
        }
        dispatcher.setHooks(hooks);

        // invocation handler
        if (SYNC == asyncMode) {
            return Reflects.newProxy(interfaceClass, new SyncInvoker(client, dispatcher));
        }
        if (ASYNC_CALLBACK == asyncMode) {
            dispatcher.setListener(checkNotNull(listener, "listener"));
            return Reflects.newProxy(interfaceClass, new AsyncInvoker(client, dispatcher));
        }

        throw new IllegalStateException("AsyncMode: " + asyncMode);
    }

    protected Dispatcher asDispatcher(DispatchMode dispatchMode, ServiceMetadata metadata) {
        if (ROUND == dispatchMode) {
            return new DefaultRoundDispatcher(metadata);
        }
        if (BROADCAST == dispatchMode) {
            return new DefaultBroadcastDispatcher(metadata);
        }

        throw new IllegalStateException("DispatchMode: " + dispatchMode);
    }
}
