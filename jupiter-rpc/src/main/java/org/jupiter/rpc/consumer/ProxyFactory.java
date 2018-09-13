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

import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Proxies;
import org.jupiter.common.util.Strings;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.AsyncInvoker;
import org.jupiter.rpc.consumer.invoker.SyncInvoker;
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
 * Proxy factory
 *
 * Consumer对象代理工厂, [group, providerName, version]
 *
 * jupiter
 * org.jupiter.rpc.consumer
 *
 * @author jiachun.fjc
 */
public class ProxyFactory<I> {

    // 接口类型
    private final Class<I> interfaceClass;
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

    public static <I> ProxyFactory<I> factory(Class<I> interfaceClass) {
        ProxyFactory<I> factory = new ProxyFactory<>(interfaceClass);
        // 初始化数据
        factory.addresses = Lists.newArrayList();
        factory.interceptors = Lists.newArrayList();
        factory.methodSpecialConfigs = Lists.newArrayList();

        return factory;
    }

    private ProxyFactory(Class<I> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Class<I> getInterfaceClass() {
        return interfaceClass;
    }

    public ProxyFactory<I> group(String group) {
        this.group = group;
        return this;
    }

    public ProxyFactory<I> providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public ProxyFactory<I> version(String version) {
        this.version = version;
        return this;
    }

    public ProxyFactory<I> directory(Directory directory) {
        return group(directory.getGroup())
                .providerName(directory.getServiceProviderName())
                .version(directory.getVersion());
    }

    public ProxyFactory<I> client(JClient client) {
        this.client = client;
        return this;
    }

    public ProxyFactory<I> serializerType(SerializerType serializerType) {
        this.serializerType = serializerType;
        return this;
    }

    public ProxyFactory<I> loadBalancerType(LoadBalancerType loadBalancerType) {
        this.loadBalancerType = loadBalancerType;
        return this;
    }

    public ProxyFactory<I> loadBalancerType(LoadBalancerType loadBalancerType, String extLoadBalancerName) {
        this.loadBalancerType = loadBalancerType;
        this.extLoadBalancerName = extLoadBalancerName;
        return this;
    }

    public ProxyFactory<I> addProviderAddress(UnresolvedAddress... addresses) {
        Collections.addAll(this.addresses, addresses);
        return this;
    }

    public ProxyFactory<I> addProviderAddress(List<UnresolvedAddress> addresses) {
        this.addresses.addAll(addresses);
        return this;
    }

    public ProxyFactory<I> invokeType(InvokeType invokeType) {
        this.invokeType = checkNotNull(invokeType);
        return this;
    }

    public ProxyFactory<I> dispatchType(DispatchType dispatchType) {
        this.dispatchType = checkNotNull(dispatchType);
        return this;
    }

    public ProxyFactory<I> timeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public ProxyFactory<I> addMethodSpecialConfig(MethodSpecialConfig... methodSpecialConfigs) {
        Collections.addAll(this.methodSpecialConfigs, methodSpecialConfigs);
        return this;
    }

    public ProxyFactory<I> addInterceptor(ConsumerInterceptor... interceptors) {
        Collections.addAll(this.interceptors, interceptors);
        return this;
    }

    public ProxyFactory<I> clusterStrategy(ClusterInvoker.Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public ProxyFactory<I> failoverRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public I newProxyInstance() {
        // check arguments
        checkNotNull(interfaceClass, "interfaceClass");

        ServiceProvider annotation = interfaceClass.getAnnotation(ServiceProvider.class);

        if (annotation != null) {
            checkArgument(
                    group == null,
                    interfaceClass.getName() + " has a @ServiceProvider annotation, can't set [group] again"
            );
            checkArgument(
                    providerName == null,
                    interfaceClass.getName() + " has a @ServiceProvider annotation, can't set [providerName] again"
            );

            group = annotation.group();
            String name = annotation.name();
            providerName = Strings.isNotBlank(name) ? name : interfaceClass.getName();
        }

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
        Object handler;
        switch (invokeType) {
            case SYNC:
                handler = new SyncInvoker(client.appName(), metadata, dispatcher, strategyConfig, methodSpecialConfigs);
                break;
            case ASYNC:
                handler = new AsyncInvoker(client.appName(), metadata, dispatcher, strategyConfig, methodSpecialConfigs);
                break;
            default:
                throw reject("invokeType: " + invokeType);
        }

        return Proxies.getDefault().newProxy(interfaceClass, handler);
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
