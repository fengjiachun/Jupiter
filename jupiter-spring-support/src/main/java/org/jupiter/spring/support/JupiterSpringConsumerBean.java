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
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

/**
 * Consumer bean, 负责构造并初始化 consumer 代理对象.
 *
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringConsumerBean<T> implements FactoryBean<T>, InitializingBean {

    private static final ConsumerHook[] EMPTY_HOOKS = new ConsumerHook[0];

    private JupiterSpringConnector connector;
    private Class<T> interfaceClass;                        // 接口类型
    private SerializerType serializerType;                  // 序列化/反序列化方式
    private long waitForAvailableTimeoutMillis = -1;        // 如果大于0, 表示阻塞等待直到连接可用并且该值为等待时间

    private transient T proxy;                              // consumer代理对象

    private InvokeType invokeType;                          // 调用方式 [同步; 异步promise; 异步callback]
    private DispatchType dispatchType;                      // 派发方式 [单播; 组播]
    private long timeoutMillis;                             // 调用超时时间设置
    private Map<String, Long> methodsSpecialTimeoutMillis;  // 指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private ConsumerHook[] hooks = EMPTY_HOOKS;             // consumer hook
    private String providerAddresses;                       // provider地址列表(IP直连)

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
        ProxyFactory<T> factory = ProxyFactory.factory(interfaceClass);

        if (serializerType != null) {
            factory.serializerType(serializerType);
        }

        if (connector.isHasRegistryServer()) {
            // 自动管理可用连接
            JConnector.ConnectionManager manager = connector.getConnector().manageConnections(interfaceClass);
            if (waitForAvailableTimeoutMillis > 0) {
                // 等待连接可用
                if (!manager.waitForAvailable(waitForAvailableTimeoutMillis)) {
                    throw new ConnectFailedException();
                }
            }
        } else {
            if (Strings.isBlank(providerAddresses)) {
                throw new IllegalArgumentException("provider addresses could not be empty");
            }

            String[] array = Strings.split(providerAddresses, ',');
            List<UnresolvedAddress> addresses = Lists.newArrayList();
            for (String s : array) {
                String[] addressStr = Strings.split(s, ':');
                String host = addressStr[0];
                int port = Integer.parseInt(addressStr[1]);
                UnresolvedAddress address = new UnresolvedAddress(host, port);
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

        if (methodsSpecialTimeoutMillis != null) {
            for (Map.Entry<String, Long> entry : methodsSpecialTimeoutMillis.entrySet()) {
                factory.methodSpecialTimeoutMillis(entry.getKey(), entry.getValue());
            }
        }

        if (hooks.length > 0) {
            factory.addHook(hooks);
        }

        proxy = factory
                .connector(connector.getConnector())  // sets connector
                .newProxyInstance();
    }

    public JupiterSpringConnector getConnector() {
        return connector;
    }

    public void setConnector(JupiterSpringConnector connector) {
        this.connector = connector;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public SerializerType getSerializerType() {
        return serializerType;
    }

    public void setSerializerType(String serializerType) {
        this.serializerType = SerializerType.parse(serializerType);
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

    public void setInvokeType(InvokeType invokeType) {
        this.invokeType = invokeType;
    }

    public DispatchType getDispatchType() {
        return dispatchType;
    }

    public void setDispatchType(DispatchType dispatchType) {
        this.dispatchType = dispatchType;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public Map<String, Long> getMethodsSpecialTimeoutMillis() {
        return methodsSpecialTimeoutMillis;
    }

    public void setMethodsSpecialTimeoutMillis(Map<String, Long> methodsSpecialTimeoutMillis) {
        this.methodsSpecialTimeoutMillis = methodsSpecialTimeoutMillis;
    }

    public ConsumerHook[] getHooks() {
        return hooks;
    }

    public void setHooks(ConsumerHook[] hooks) {
        this.hooks = hooks;
    }

    public String getProviderAddresses() {
        return providerAddresses;
    }

    public void setProviderAddresses(String providerAddresses) {
        this.providerAddresses = providerAddresses;
    }
}
