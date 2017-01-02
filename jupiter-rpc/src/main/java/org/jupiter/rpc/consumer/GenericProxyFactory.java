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
import org.jupiter.rpc.ConsumerHook;
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailFastClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailOverClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailSafeClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.AsyncGenericInvoker;
import org.jupiter.rpc.consumer.invoker.GenericInvoker;
import org.jupiter.rpc.consumer.invoker.SyncGenericInvoker;
import org.jupiter.rpc.load.balance.LoadBalancerFactory;
import org.jupiter.rpc.load.balance.LoadBalancerType;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.Directory;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.UnresolvedAddress;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.DispatchType.ROUND;
import static org.jupiter.rpc.InvokeType.ASYNC;
import static org.jupiter.rpc.InvokeType.SYNC;
import static org.jupiter.rpc.load.balance.LoadBalancerType.RANDOM;
import static org.jupiter.serialization.SerializerType.PROTO_STUFF;

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

    // 服务组别
    private String group;
    // 服务版本号, 通常在接口不兼容时版本号才需要升级
    private String version;
    // 服务名称
    private String providerName;

    // jupiter client
    private JClient client;
    // 序列化/反序列化方式
    private SerializerType serializerType = PROTO_STUFF;
    // 软负载均衡类型
    private LoadBalancerType loadBalancerType = RANDOM;
    // provider地址
    private List<UnresolvedAddress> addresses;
    // 调用方式 [同步; 异步]
    private InvokeType invokeType = SYNC;
    // 派发方式 [单播; 组播]
    private DispatchType dispatchType = ROUND;
    // 调用超时时间设置
    private long timeoutMillis;
    // 指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodsSpecialTimeoutMillis;
    // 消费者端钩子函数
    private List<ConsumerHook> hooks;
    // 集群容错策略
    private ClusterInvoker.Strategy strategy = ClusterInvoker.Strategy.FAIL_FAST;
    // failover重试次数
    private int retries = 2;

    public static GenericProxyFactory factory() {
        GenericProxyFactory factory = new GenericProxyFactory();
        // 初始化数据
        factory.addresses = Lists.newArrayList();
        factory.hooks = Lists.newArrayList();
        factory.methodsSpecialTimeoutMillis = Maps.newHashMap();

        return factory;
    }

    private GenericProxyFactory() {}

    /**
     * Sets the jupiter client.
     */
    public GenericProxyFactory client(JClient client) {
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
     * Sets the service serializer type.
     */
    public GenericProxyFactory serializerType(SerializerType serializerType) {
        this.serializerType = serializerType;
        return this;
    }

    /**
     * Sets the service load balancer type.
     */
    public GenericProxyFactory loadBalancerType(LoadBalancerType loadBalancerType) {
        this.loadBalancerType = loadBalancerType;
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
    public GenericProxyFactory timeoutMillis(long timeoutMillis) {
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
     * Adds hooks.
     */
    public GenericProxyFactory addHook(ConsumerHook... hooks) {
        Collections.addAll(this.hooks, hooks);
        return this;
    }

    /**
     * Sets cluster strategy, only support ROUND & SYNC mode.
     */
    public GenericProxyFactory clusterStrategy(ClusterInvoker.Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * Sets failover strategy's retries.
     */
    public GenericProxyFactory failoverRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public GenericInvoker newProxyInstance() {
        // check arguments
        checkNotNull(client, "client");
        checkNotNull(group, "group");
        checkNotNull(version, "version");
        checkNotNull(providerName, "providerName");
        checkNotNull(serializerType, "serializerType");

        if (dispatchType == BROADCAST && invokeType != ASYNC) {
            throw new UnsupportedOperationException("illegal type, BROADCAST only support ASYNC");
        }

        // metadata
        ServiceMetadata metadata = new ServiceMetadata(group, version, providerName);

        JConnector<JConnection> connector = client.connector();
        for (UnresolvedAddress address : addresses) {
            connector.addChannelGroup(metadata, connector.group(address));
        }

        // dispatcher
        Dispatcher dispatcher = asDispatcher(metadata, serializerType)
                .hooks(hooks)
                .timeoutMillis(timeoutMillis)
                .methodsSpecialTimeoutMillis(methodsSpecialTimeoutMillis);

        switch (invokeType) {
            case SYNC:
                return new SyncGenericInvoker(asClusterInvoker(strategy, dispatcher));
            case ASYNC:
                return new AsyncGenericInvoker(asClusterInvoker(strategy, dispatcher));
            default:
                throw new IllegalStateException("InvokeType: " + invokeType);
        }
    }

    protected Dispatcher asDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        switch (dispatchType) {
            case ROUND:
                return new DefaultRoundDispatcher(
                        LoadBalancerFactory.getInstance(loadBalancerType), metadata, serializerType);
            case BROADCAST:
                return new DefaultBroadcastDispatcher(null, metadata, serializerType);
            default:
                throw new IllegalStateException("DispatchType: " + dispatchType);
        }
    }

    private ClusterInvoker asClusterInvoker(ClusterInvoker.Strategy strategy, Dispatcher dispatcher) {
        switch (strategy) {
            case FAIL_FAST:
                return new FailFastClusterInvoker(client, dispatcher);
            case FAIL_OVER:
                return new FailOverClusterInvoker(client, dispatcher, retries);
            case FAIL_SAFE:
                return new FailSafeClusterInvoker(client, dispatcher);
            default:
                throw new IllegalStateException("Strategy: " + strategy);
        }
    }
}
