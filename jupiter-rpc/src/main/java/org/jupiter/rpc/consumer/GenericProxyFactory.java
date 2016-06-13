/*
 * Copyright (c) 2016 The Jupiter Project
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
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.CallbackGenericInvoker;
import org.jupiter.rpc.consumer.invoker.PromiseGenericInvoker;
import org.jupiter.rpc.consumer.invoker.GenericInvoker;
import org.jupiter.rpc.consumer.invoker.SyncGenericInvoker;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.DispatchType.ROUND;
import static org.jupiter.rpc.InvokeType.CALLBACK;
import static org.jupiter.rpc.InvokeType.SYNC;

/**
 * 泛化ProxyFactory
 *
 * 简单的理解, 泛化调用就是不依赖二方包, 通过传入方法名, 方法参数值, 就可以调用服务.
 *
 * jupiter
 * org.jupiter.rpc.consumer
 *
 * @author jiachun.fjc
 */
public class GenericProxyFactory {

    private String group;
    private String version;
    private String providerName;

    private JClient client;
    private List<UnresolvedAddress> addresses;
    private InvokeType invokeType = SYNC;
    private DispatchType dispatchType = ROUND;
    private int timeoutMillis;
    private Map<String, Long> methodsSpecialTimeoutMillis;
    private JListener listener;
    private List<ConsumerHook> hooks;

    public static GenericProxyFactory factory() {
        GenericProxyFactory factory = new GenericProxyFactory();
        // 初始化数据
        factory.addresses = Lists.newArrayList();
        factory.hooks = Lists.newArrayList();
        factory.methodsSpecialTimeoutMillis = Maps.newTreeMap();

        return factory;
    }

    private GenericProxyFactory() {}

    /**
     * Sets the connector.
     */
    public GenericProxyFactory connector(JClient client) {
        this.client = client;
        return this;
    }

    /**
     * Sets the group.
     */
    public GenericProxyFactory group(String group) {
        this.group = group;
        return this;
    }

    /**
     * Sets the version.
     */
    public GenericProxyFactory version(String version) {
        this.version = version;
        return this;
    }

    /**
     * Sets the service provider name.
     */
    public GenericProxyFactory providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    /**
     * Sets the group, version and service provider name.
     */
    public GenericProxyFactory directory(Directory directory) {
        return group(directory.getGroup())
                .version(directory.getVersion())
                .providerName(directory.getServiceProviderName());
    }

    /**
     * Adds provider's addresses.
     */
    public GenericProxyFactory addProviderAddress(UnresolvedAddress... addresses) {
        Collections.addAll(this.addresses, addresses);
        return this;
    }

    /**
     * Adds provider's addresses.
     */
    public GenericProxyFactory addProviderAddress(List<UnresolvedAddress> addresses) {
        this.addresses.addAll(addresses);
        return this;
    }

    /**
     * Synchronous blocking, asynchronous with future or asynchronous with callback,
     * the default is synchronous.
     */
    public GenericProxyFactory invokeType(InvokeType invokeType) {
        this.invokeType = checkNotNull(invokeType);
        return this;
    }

    /**
     * Sets the type of dispatch, the default is {@link DispatchType#ROUND}
     */
    public GenericProxyFactory dispatchType(DispatchType dispatchType) {
        this.dispatchType = checkNotNull(dispatchType);
        return this;
    }

    /**
     * Timeout milliseconds.
     */
    public GenericProxyFactory timeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    /**
     * Method special timeout milliseconds.
     */
    public GenericProxyFactory methodSpecialTimeoutMillis(String methodName, long timeoutMillis) {
        methodsSpecialTimeoutMillis.put(methodName, timeoutMillis);
        return this;
    }

    /**
     * Asynchronous callback listener.
     */
    public GenericProxyFactory listener(JListener listener) {
        if (invokeType != CALLBACK) {
            throw new UnsupportedOperationException("InvokeType should first be set to CALLBACK");
        }
        this.listener = listener;
        return this;
    }

    /**
     * Adds hooks.
     */
    public GenericProxyFactory addHook(ConsumerHook... hooks) {
        Collections.addAll(this.hooks, hooks);
        return this;
    }

    public GenericInvoker newProxyInstance() {
        // check arguments
        checkNotNull(client, "connector");
        checkNotNull(group, "group");
        checkNotNull(version, "version");
        checkNotNull(providerName, "providerName");
        if (dispatchType == BROADCAST && invokeType != CALLBACK) {
            throw new UnsupportedOperationException("illegal type, BROADCAST only support CALLBACK");
        }

        // metadata
        ServiceMetadata metadata = new ServiceMetadata(group, version, providerName);

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

        switch (invokeType) {
            case SYNC:
                return new SyncGenericInvoker(client, dispatcher);
            case PROMISE:
                return new PromiseGenericInvoker(client, dispatcher);
            case CALLBACK:
                dispatcher.setListener(checkNotNull(listener, "listener"));
                return new CallbackGenericInvoker(client, dispatcher);
            default:
                throw new IllegalStateException("InvokeType: " + invokeType);
        }
    }

    protected Dispatcher asDispatcher(ServiceMetadata metadata) {
        switch (dispatchType) {
            case ROUND:
                return new DefaultRoundDispatcher(metadata);
            case BROADCAST:
                return new DefaultBroadcastDispatcher(metadata);
            default:
                throw new IllegalStateException("DispatchType: " + dispatchType);
        }
    }
}
