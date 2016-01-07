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
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.aop.ConsumerHook;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.AsyncGenericInvoker;
import org.jupiter.rpc.consumer.invoker.GenericInvoker;
import org.jupiter.rpc.consumer.invoker.SyncGenericInvoker;
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
 * 泛化ProxyFactory
 *
 * 简单的理解, 泛化调用就是不依赖二方包, 通过传入方法名, 方法参数值, 就可以调用服务。
 *
 * jupiter
 * org.jupiter.rpc.consumer
 *
 * @author jiachun.fjc
 */
public class GenericProxyFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GenericProxyFactory.class);

    private JClient client;
    private List<UnresolvedAddress> addresses;
    private String group;
    private String version;
    private String providerName;
    private AsyncMode asyncMode = SYNC;
    private DispatchMode dispatchMode = ROUND;
    private int timeoutMills;
    private List<ConsumerHook> hooks;
    private JListener listener;

    public static GenericProxyFactory factory() {
        GenericProxyFactory fac = new GenericProxyFactory();

        // 初始化数据
        fac.addresses = Lists.newArrayList();
        fac.hooks = Lists.newArrayList(logConsumerHook);

        return fac;
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
     * Synchronous blocking or asynchronous callback, the default is synchronous.
     */
    public GenericProxyFactory asyncMode(AsyncMode asyncMode) {
        this.asyncMode = checkNotNull(asyncMode);
        return this;
    }

    /**
     * Sets the mode of dispatch, the default is {@link DispatchMode#ROUND}
     */
    public GenericProxyFactory dispatchMode(DispatchMode dispatchMode) {
        this.dispatchMode = checkNotNull(dispatchMode);
        return this;
    }

    /**
     * Timeout milliseconds.
     */
    public GenericProxyFactory timeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
        return this;
    }

    /**
     * Asynchronous callback listener.
     */
    public GenericProxyFactory listener(JListener listener) {
        if (asyncMode != ASYNC_CALLBACK) {
            throw new UnsupportedOperationException("asyncMode should first be set to ASYNC_CALLBACK");
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

    @SuppressWarnings("unchecked")
    public GenericInvoker newProxyInstance() {
        // check arguments
        checkNotNull(client, "connector");
        checkNotNull(group, "group");
        checkNotNull(version, "version");
        checkNotNull(providerName, "providerName");
        checkArgument(!(asyncMode == SYNC && dispatchMode == BROADCAST), "illegal mode, [SYNC & BROADCAST] unsupported");

        // metadata
        ServiceMetadata metadata = new ServiceMetadata(group, version, providerName);

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
        GenericInvoker genericInvoker = null;
        switch (asyncMode) {
            case SYNC:
                genericInvoker = new SyncGenericInvoker(client, dispatcher);
                break;
            case ASYNC_CALLBACK:
                dispatcher.setListener(checkNotNull(listener, "listener"));
                genericInvoker = new AsyncGenericInvoker(client, dispatcher);
                break;
        }

        return genericInvoker;
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
