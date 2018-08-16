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

import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Strings;
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.AsyncGenericInvoker;
import org.jupiter.rpc.consumer.invoker.GenericInvoker;
import org.jupiter.rpc.consumer.invoker.SyncGenericInvoker;
import org.jupiter.rpc.load.balance.LoadBalancerFactory;
import org.jupiter.rpc.load.balance.LoadBalancerType;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.Directory;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.UnresolvedAddress;

import java.util.Collections;
import java.util.List;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;

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
    // 服务名称
    private String providerName;
    // 服务版本号, 通常在接口不兼容时版本号才需要升级
    private String version;

    // jupiter client
    private JClient client;
    // 序列化/反序列化方式
    private SerializerType serializerType = SerializerType.getDefault();
    // 软负载均衡类型
    private LoadBalancerType loadBalancerType = LoadBalancerType.getDefault();
    // 基于ExtSpiLoadBalancerFactory扩展的负载均衡可以选择指定名字, 可以利用名字作为唯一标识扩展多种类型的负载均衡
    private String extLoadBalancerName;
    // provider地址
    private List<UnresolvedAddress> addresses;
    // 调用方式 [同步, 异步]
    private InvokeType invokeType = InvokeType.getDefault();
    // 派发方式 [单播, 广播]
    private DispatchType dispatchType = DispatchType.getDefault();
    // 调用超时时间设置
    private long timeoutMillis;
    // 指定方法的单独配置, 方法参数类型不做区别对待
    private List<MethodSpecialConfig> methodSpecialConfigs;
    // 消费者端拦截器
    private List<ConsumerInterceptor> interceptors;
    // 集群容错策略
    private ClusterInvoker.Strategy strategy = ClusterInvoker.Strategy.getDefault();
    // failover重试次数
    private int retries = 2;

    public static GenericProxyFactory factory() {
        GenericProxyFactory factory = new GenericProxyFactory();
        // 初始化数据
        factory.addresses = Lists.newArrayList();
        factory.interceptors = Lists.newArrayList();
        factory.methodSpecialConfigs = Lists.newArrayList();

        return factory;
    }

    private GenericProxyFactory() {}

    public GenericProxyFactory group(String group) {
        this.group = group;
        return this;
    }

    public GenericProxyFactory providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public GenericProxyFactory version(String version) {
        this.version = version;
        return this;
    }

    public GenericProxyFactory directory(Directory directory) {
        return group(directory.getGroup())
                .providerName(directory.getServiceProviderName())
                .version(directory.getVersion());
    }

    public GenericProxyFactory client(JClient client) {
        this.client = client;
        return this;
    }

    public GenericProxyFactory serializerType(SerializerType serializerType) {
        this.serializerType = serializerType;
        return this;
    }

    public GenericProxyFactory loadBalancerType(LoadBalancerType loadBalancerType) {
        this.loadBalancerType = loadBalancerType;
        return this;
    }

    public GenericProxyFactory loadBalancerType(LoadBalancerType loadBalancerType, String extLoadBalancerName) {
        this.loadBalancerType = loadBalancerType;
        this.extLoadBalancerName = extLoadBalancerName;
        return this;
    }

    public GenericProxyFactory addProviderAddress(UnresolvedAddress... addresses) {
        Collections.addAll(this.addresses, addresses);
        return this;
    }

    public GenericProxyFactory addProviderAddress(List<UnresolvedAddress> addresses) {
        this.addresses.addAll(addresses);
        return this;
    }

    public GenericProxyFactory invokeType(InvokeType invokeType) {
        this.invokeType = checkNotNull(invokeType);
        return this;
    }

    public GenericProxyFactory dispatchType(DispatchType dispatchType) {
        this.dispatchType = checkNotNull(dispatchType);
        return this;
    }

    public GenericProxyFactory timeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public GenericProxyFactory addMethodSpecialConfig(MethodSpecialConfig... methodSpecialConfigs) {
        Collections.addAll(this.methodSpecialConfigs, methodSpecialConfigs);
        return this;
    }

    public GenericProxyFactory addInterceptor(ConsumerInterceptor... interceptors) {
        Collections.addAll(this.interceptors, interceptors);
        return this;
    }

    public GenericProxyFactory clusterStrategy(ClusterInvoker.Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public GenericProxyFactory failoverRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public GenericInvoker newProxyInstance() {
        // check arguments
        checkArgument(Strings.isNotBlank(group), "group");
        checkArgument(Strings.isNotBlank(providerName), "providerName");
        checkNotNull(client, "client");
        checkNotNull(serializerType, "serializerType");

        if (dispatchType == DispatchType.BROADCAST && invokeType == InvokeType.SYNC) {
            throw reject("broadcast & sync unsupported");
        }

        // metadata
        ServiceMetadata metadata = new ServiceMetadata(
                group,
                providerName,
                Strings.isNotBlank(version) ? version : JConstants.DEFAULT_VERSION
        );

        JConnector<JConnection> connector = client.connector();
        for (UnresolvedAddress address : addresses) {
            connector.addChannelGroup(metadata, connector.group(address));
        }

        // dispatcher
        Dispatcher dispatcher = dispatcher()
                .interceptors(interceptors)
                .timeoutMillis(timeoutMillis)
                .methodSpecialConfigs(methodSpecialConfigs);

        ClusterStrategyConfig strategyConfig = ClusterStrategyConfig.of(strategy, retries);
        switch (invokeType) {
            case SYNC:
                return new SyncGenericInvoker(client.appName(), metadata, dispatcher, strategyConfig, methodSpecialConfigs);
            case ASYNC:
                return new AsyncGenericInvoker(client.appName(), metadata, dispatcher, strategyConfig, methodSpecialConfigs);
            default:
                throw reject("invokeType: " + invokeType);
        }
    }

    protected Dispatcher dispatcher() {
        switch (dispatchType) {
            case ROUND:
                return new DefaultRoundDispatcher(
                        client,
                        LoadBalancerFactory.getInstance(loadBalancerType, extLoadBalancerName), serializerType);
            case BROADCAST:
                return new DefaultBroadcastDispatcher(client, serializerType);
            default:
                throw reject("dispatchType: " + dispatchType);
        }
    }

    private static UnsupportedOperationException reject(String message) {
        return new UnsupportedOperationException(message);
    }
}
