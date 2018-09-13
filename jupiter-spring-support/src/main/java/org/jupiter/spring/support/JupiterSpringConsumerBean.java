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

package org.jupiter.spring.support;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Strings;
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.consumer.ConsumerInterceptor;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.load.balance.LoadBalancerType;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.UnresolvedSocketAddress;
import org.jupiter.transport.exception.ConnectFailedException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * Consumer bean, 负责构造并初始化 consumer 代理对象.
 *
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringConsumerBean<T> implements FactoryBean<T>, InitializingBean {

    private JupiterSpringClient client;

    private Class<T> interfaceClass;                            // 服务接口类型

    private String version;                                     // 服务版本号, 通常在接口不兼容时版本号才需要升级
    private SerializerType serializerType;                      // 序列化/反序列化方式
    private LoadBalancerType loadBalancerType;                  // 软负载均衡类型
    private String extLoadBalancerName;                         // 扩展软负载均衡唯一标识
    private long waitForAvailableTimeoutMillis = -1;            // 如果大于0, 表示阻塞等待直到连接可用并且该值为等待时间

    private transient T proxy;                                  // consumer代理对象

    private InvokeType invokeType;                              // 调用方式 [同步, 异步]
    private DispatchType dispatchType;                          // 派发方式 [单播, 广播]
    private long timeoutMillis;                                 // 调用超时时间设置
    private List<MethodSpecialConfig> methodSpecialConfigs;     // 指定方法的单独配置, 方法参数类型不做区别对待
    private ConsumerInterceptor[] consumerInterceptors;         // 消费者端拦截器
    private String providerAddresses;                           // provider地址列表, 逗号分隔(IP直连)
    private ClusterInvoker.Strategy clusterStrategy;            // 集群容错策略
    private int failoverRetries;                                // failover重试次数(只对ClusterInvoker.Strategy.FAIL_OVER有效)

    @Override
    public T getObject() throws Exception {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        ProxyFactory<T> factory = ProxyFactory.factory(interfaceClass)
                .version(version);

        if (serializerType != null) {
            factory.serializerType(serializerType);
        }

        if (loadBalancerType != null) {
            factory.loadBalancerType(loadBalancerType, extLoadBalancerName);
        }

        if (client.isHasRegistryServer()) {
            // 自动管理可用连接
            JConnector.ConnectionWatcher watcher = client.getClient().watchConnections(interfaceClass, version);
            if (waitForAvailableTimeoutMillis > 0) {
                // 等待连接可用
                if (!watcher.waitForAvailable(waitForAvailableTimeoutMillis)) {
                    throw new ConnectFailedException();
                }
            }
        } else {
            if (Strings.isBlank(providerAddresses)) {
                throw new IllegalArgumentException("Provider addresses could not be empty");
            }

            String[] array = Strings.split(providerAddresses, ',');
            List<UnresolvedAddress> addresses = Lists.newArrayList();
            for (String s : array) {
                String[] addressStr = Strings.split(s, ':');
                String host = addressStr[0];
                int port = Integer.parseInt(addressStr[1]);
                UnresolvedAddress address = new UnresolvedSocketAddress(host, port);
                addresses.add(address);
            }
            factory.addProviderAddress(addresses);
        }

        if (invokeType != null) {
            factory.invokeType(invokeType);
        }

        if (dispatchType != null) {
            factory.dispatchType(dispatchType);
        }

        if (timeoutMillis > 0) {
            factory.timeoutMillis(timeoutMillis);
        }

        if (methodSpecialConfigs != null) {
            for (MethodSpecialConfig config : methodSpecialConfigs) {
                factory.addMethodSpecialConfig(config);
            }
        }

        ConsumerInterceptor[] globalConsumerInterceptors = client.getGlobalConsumerInterceptors();
        if (globalConsumerInterceptors != null && globalConsumerInterceptors.length > 0) {
            factory.addInterceptor(globalConsumerInterceptors);
        }

        if (consumerInterceptors != null && consumerInterceptors.length > 0) {
            factory.addInterceptor(consumerInterceptors);
        }

        if (clusterStrategy != null) {
            factory.clusterStrategy(clusterStrategy);
        }

        if (failoverRetries > 0) {
            factory.failoverRetries(failoverRetries);
        }

        proxy = factory
                .client(client.getClient())
                .newProxyInstance();
    }

    public JupiterSpringClient getClient() {
        return client;
    }

    public void setClient(JupiterSpringClient client) {
        this.client = client;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public SerializerType getSerializerType() {
        return serializerType;
    }

    public void setSerializerType(String serializerType) {
        this.serializerType = SerializerType.parse(serializerType);
    }

    public LoadBalancerType getLoadBalancerType() {
        return loadBalancerType;
    }

    public void setLoadBalancerType(String loadBalancerType) {
        this.loadBalancerType = LoadBalancerType.parse(loadBalancerType);
        if (this.loadBalancerType == null) {
            throw new IllegalArgumentException(loadBalancerType);
        }
    }

    public String getExtLoadBalancerName() {
        return extLoadBalancerName;
    }

    public void setExtLoadBalancerName(String extLoadBalancerName) {
        this.extLoadBalancerName = extLoadBalancerName;
    }

    public long getWaitForAvailableTimeoutMillis() {
        return waitForAvailableTimeoutMillis;
    }

    public void setWaitForAvailableTimeoutMillis(long waitForAvailableTimeoutMillis) {
        this.waitForAvailableTimeoutMillis = waitForAvailableTimeoutMillis;
    }

    public InvokeType getInvokeType() {
        return invokeType;
    }

    public void setInvokeType(String invokeType) {
        this.invokeType = InvokeType.parse(invokeType);
        if (this.invokeType == null) {
            throw new IllegalArgumentException(invokeType);
        }
    }

    public DispatchType getDispatchType() {
        return dispatchType;
    }

    public void setDispatchType(String dispatchType) {
        this.dispatchType = DispatchType.parse(dispatchType);
        if (this.dispatchType == null) {
            throw new IllegalArgumentException(dispatchType);
        }
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public List<MethodSpecialConfig> getMethodSpecialConfigs() {
        return methodSpecialConfigs;
    }

    public void setMethodSpecialConfigs(List<MethodSpecialConfig> methodSpecialConfigs) {
        this.methodSpecialConfigs = methodSpecialConfigs;
    }

    public ConsumerInterceptor[] getConsumerInterceptors() {
        return consumerInterceptors;
    }

    public void setConsumerInterceptors(ConsumerInterceptor[] consumerInterceptors) {
        this.consumerInterceptors = consumerInterceptors;
    }

    public String getProviderAddresses() {
        return providerAddresses;
    }

    public void setProviderAddresses(String providerAddresses) {
        this.providerAddresses = providerAddresses;
    }

    public ClusterInvoker.Strategy getClusterStrategy() {
        return clusterStrategy;
    }

    public void setClusterStrategy(String clusterStrategy) {
        this.clusterStrategy = ClusterInvoker.Strategy.parse(clusterStrategy);
        if (this.clusterStrategy == null) {
            throw new IllegalArgumentException(clusterStrategy);
        }
    }

    public int getFailoverRetries() {
        return failoverRetries;
    }

    public void setFailoverRetries(int failoverRetries) {
        this.failoverRetries = failoverRetries;
    }
}
