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
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.annotation.ServiceProvider;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.AsyncInvoker;
import org.jupiter.rpc.consumer.invoker.SyncInvoker;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import java.lang.reflect.InvocationHandler;
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
public class ProxyFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyFactory.class);

    private JClient client;
    private List<UnresolvedAddress> addresses;
    private Class<?> serviceInterface;
    private AsyncMode asyncMode = SYNC;
    private DispatchMode dispatchMode = ROUND;
    private int timeoutMills;
    private List<ConsumerHook> hooks;
    private JListener listener;

    public static ProxyFactory factory() {
        ProxyFactory fac = new ProxyFactory();

        // 初始化数据
        fac.addresses = Lists.newArrayList();
        fac.hooks = Lists.newArrayList(logConsumerHook);

        return fac;
    }

    private ProxyFactory() {}

    /**
     * Sets the connector.
     */
    public ProxyFactory connector(JClient client) {
        this.client = client;
        return this;
    }

    /**
     * Adds provider's addresses.
     */
    public ProxyFactory addProviderAddress(UnresolvedAddress... addresses) {
        Collections.addAll(this.addresses, addresses);
        return this;
    }

    /**
     * Adds provider's addresses.
     */
    public ProxyFactory addProviderAddress(List<UnresolvedAddress> addresses) {
        this.addresses.addAll(addresses);
        return this;
    }

    /**
     * Sets the service interface type.
     */
    public <I> ProxyFactory interfaceClass(Class<I> serviceInterface) {
        this.serviceInterface = serviceInterface;
        return this;
    }

    /**
     * Synchronous blocking or asynchronous callback, the default is synchronous.
     */
    public ProxyFactory asyncMode(AsyncMode asyncMode) {
        this.asyncMode = checkNotNull(asyncMode);
        return this;
    }

    /**
     * Sets the mode of dispatch, the default is {@link DispatchMode#ROUND}
     */
    public ProxyFactory dispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = checkNotNull(dispatchMode);
        return this;
    }

    /**
     * Timeout milliseconds.
     */
    public ProxyFactory timeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
        return this;
    }

    /**
     * Asynchronous callback listener.
     */
    public ProxyFactory listener(JListener listener) {
        if (asyncMode != ASYNC_CALLBACK) {
            throw new UnsupportedOperationException("asyncMode should first be set to ASYNC_CALLBACK");
        }
        this.listener = listener;
        return this;
    }

    /**
     * Adds hooks.
     */
    public ProxyFactory addHook(ConsumerHook... hooks) {
        Collections.addAll(this.hooks, hooks);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <I> I newProxyInstance() {
        // check arguments
        checkNotNull(client, "connector");
        checkNotNull(serviceInterface, "serviceInterface");
        checkArgument(!(asyncMode == SYNC && dispatchMode == BROADCAST), "illegal mode, [SYNC & BROADCAST] unsupported");
        ServiceProvider annotation = serviceInterface.getAnnotation(ServiceProvider.class);
        checkNotNull(annotation, serviceInterface + " is not a ServiceProvider interface");
        String providerName = annotation.value();
        providerName = Strings.isNotBlank(providerName) ? providerName : serviceInterface.getSimpleName();

        // metadata
        ServiceMetadata metadata = new ServiceMetadata(annotation.group(), annotation.version(), providerName);

        for (UnresolvedAddress address : addresses) {
            client.addChannelGroup(metadata, client.group(address));
        }

        // dispatcher
        Dispatcher dispatcher = null;
        switch (dispatchMode) {
            case ROUND:
                dispatcher = new DefaultRoundDispatcher(metadata);
                break;
            case BROADCAST:
                dispatcher = new DefaultBroadcastDispatcher(metadata);
                break;
        }
        if (timeoutMills > 0) {
            dispatcher.setTimeoutMills(timeoutMills);
        }
        dispatcher.setHooks(hooks);

        // invocation handler
        InvocationHandler handler = null;
        switch (asyncMode) {
            case SYNC:
                handler = new SyncInvoker(client, dispatcher);
                break;
            case ASYNC_CALLBACK:
                dispatcher.setListener(checkNotNull(listener, "listener"));
                handler = new AsyncInvoker(client, dispatcher);
                break;
        }

        return (I) Reflects.newProxy(serviceInterface, handler);
    }

    private static final ConsumerHook logConsumerHook = new ConsumerHook() {

        @Override
        public void before(JRequest request) {
            logger.debug("Request: [{}], {}.", request.invokeId(), request.message());
        }

        @Override
        public void after(JRequest request) {
            logger.debug("Request: [{}], has respond.", request.invokeId());
        }
    };
}
