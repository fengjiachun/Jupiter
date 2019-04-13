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

import java.util.concurrent.Executor;

import org.jupiter.common.util.Requires;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringProviderBean implements InitializingBean, ApplicationContextAware {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JupiterSpringProviderBean.class);

    private ServiceWrapper serviceWrapper;                      // 服务元信息

    private JupiterSpringServer server;

    private Object providerImpl;                                // 服务对象
    private ProviderInterceptor[] providerInterceptors;         // 私有拦截器
    private int weight;                                         // 权重
    private Executor executor;                                  // 该服务私有的线程池
    private FlowController<JRequest> flowController;            // 该服务私有的流量控制器
    private JServer.ProviderInitializer<?> providerInitializer; // 服务延迟初始化
    private Executor providerInitializerExecutor;               // 服务私有的延迟初始化线程池

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).addApplicationListener(new JupiterApplicationListener());
        }
    }

    private void init() throws Exception {
        Requires.requireNotNull(server, "server");

        JServer.ServiceRegistry registry = server.getServer().serviceRegistry();

        if (providerInterceptors != null && providerInterceptors.length > 0) {
            registry.provider(providerImpl, providerInterceptors);
        } else {
            registry.provider(providerImpl);
        }

        serviceWrapper = registry
                .weight(weight)
                .executor(executor)
                .flowController(flowController)
                .register();
    }

    public JupiterSpringServer getServer() {
        return server;
    }

    public void setServer(JupiterSpringServer server) {
        this.server = server;
    }

    public Object getProviderImpl() {
        return providerImpl;
    }

    public void setProviderImpl(Object providerImpl) {
        this.providerImpl = providerImpl;
    }

    public ProviderInterceptor[] getProviderInterceptors() {
        return providerInterceptors;
    }

    public void setProviderInterceptors(ProviderInterceptor[] providerInterceptors) {
        this.providerInterceptors = providerInterceptors;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public FlowController<JRequest> getFlowController() {
        return flowController;
    }

    public void setFlowController(FlowController<JRequest> flowController) {
        this.flowController = flowController;
    }

    public JServer.ProviderInitializer<?> getProviderInitializer() {
        return providerInitializer;
    }

    public void setProviderInitializer(JServer.ProviderInitializer<?> providerInitializer) {
        this.providerInitializer = providerInitializer;
    }

    public Executor getProviderInitializerExecutor() {
        return providerInitializerExecutor;
    }

    public void setProviderInitializerExecutor(Executor providerInitializerExecutor) {
        this.providerInitializerExecutor = providerInitializerExecutor;
    }

    private final class JupiterApplicationListener implements ApplicationListener {

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (server.isHasRegistryServer() && event instanceof ContextRefreshedEvent) {
                // 发布服务
                if (providerInitializer == null) {
                    server.getServer().publish(serviceWrapper);
                } else {
                    server.getServer().publishWithInitializer(
                            serviceWrapper, providerInitializer, providerInitializerExecutor);
                }

                logger.info("#publish service: {}.", serviceWrapper);
            }
        }
    }
}
