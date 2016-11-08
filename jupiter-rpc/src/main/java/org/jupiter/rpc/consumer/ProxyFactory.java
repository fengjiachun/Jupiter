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
import org.jupiter.common.util.Proxies;
import org.jupiter.common.util.Strings;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.CallbackInvoker;
import org.jupiter.rpc.consumer.invoker.PromiseInvoker;
import org.jupiter.rpc.consumer.invoker.SyncInvoker;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.SerializerType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.DispatchType.ROUND;
import static org.jupiter.rpc.InvokeType.CALLBACK;
import static org.jupiter.rpc.InvokeType.SYNC;
import static org.jupiter.serialization.SerializerType.PROTO_STUFF;

/**
 * Proxy factory
 *
 * Consumer对象代理工厂
 *
 * jupiter
 * org.jupiter.rpc.consumer
 *
 * @author jiachun.fjc
 */
public class ProxyFactory<I> {

    private final Class<I> interfaceClass;                      // 接口类型
    private SerializerType serializerType = PROTO_STUFF;        // 序列化/反序列化方式

    private JClient client;                                     // connector
    private List<UnresolvedAddress> addresses;                  // provider地址
    private InvokeType invokeType = SYNC;                       // 调用方式 [同步; 异步promise; 异步callback]
    private DispatchType dispatchType = ROUND;                  // 派发方式 [单播; 组播]
    private long timeoutMillis;                                 // 调用超时时间设置
    private Map<String, Long> methodsSpecialTimeoutMillis;      // 指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private JListener listener;                                 // 回调函数
    private List<ConsumerHook> hooks;                           // consumer hook

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
     * Sets the service serializer type.
     */
    public ProxyFactory serializerType(SerializerType serializerType) {
        this.serializerType = serializerType;
        return this;
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
    public ProxyFactory<I> invokeType(InvokeType invokeType) {
        this.invokeType = checkNotNull(invokeType);
        return this;
    }

    /**
     * Sets the type of dispatch, the default is {@link DispatchType#ROUND}
     */
    public ProxyFactory<I> dispatchType(DispatchType dispatchType) {
        this.dispatchType = checkNotNull(dispatchType);
        return this;
    }

    /**
     * Timeout milliseconds.
     */
    public ProxyFactory<I> timeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    /**
     * Method special timeout milliseconds.
     */
    public ProxyFactory<I> methodSpecialTimeoutMillis(String methodName, long timeoutMillis) {
        methodsSpecialTimeoutMillis.put(methodName, timeoutMillis);
        return this;
    }

    /**
     * Asynchronous callback listener.
     */
    public ProxyFactory<I> listener(JListener listener) {
        if (invokeType != CALLBACK) {
            throw new UnsupportedOperationException("InvokeType should first be set to CALLBACK");
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
        checkNotNull(serializerType, "serializerType");

        if (dispatchType == BROADCAST && invokeType != CALLBACK) {
            throw new UnsupportedOperationException("illegal type, BROADCAST only support CALLBACK");
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
        Dispatcher dispatcher = asDispatcher(metadata, serializerType);
        if (timeoutMillis > 0) {
            dispatcher.setTimeoutMillis(timeoutMillis);
        }
        if (!methodsSpecialTimeoutMillis.isEmpty()) {
            dispatcher.setMethodsSpecialTimeoutMillis(methodsSpecialTimeoutMillis);
        }
        dispatcher.setHooks(hooks);

        Object handler;
        switch (invokeType) {
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
                throw new IllegalStateException("InvokeType: " + invokeType);
        }

        return Proxies.getDefault().newProxy(interfaceClass, handler);
    }

    protected Dispatcher asDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        switch (dispatchType) {
            case ROUND:
                return new DefaultRoundDispatcher(metadata, serializerType);
            case BROADCAST:
                return new DefaultBroadcastDispatcher(metadata, serializerType);
            default:
                throw new IllegalStateException("DispatchType: " + dispatchType);
        }
    }
}
