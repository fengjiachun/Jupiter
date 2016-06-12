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

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.rpc.provider.ProviderProxyHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.Executor;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringProviderBean implements InitializingBean, ApplicationContextAware {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JupiterSpringProviderBean.class);

    private static final ProviderInterceptor[] EMPTY_INTERCEPTORS = new ProviderInterceptor[0];

    private ServiceWrapper serviceWrapper;

    private JupiterSpringAcceptor acceptor;
    private Object providerImpl;
    private ProviderInterceptor[] providerInterceptors = EMPTY_INTERCEPTORS;
    private int weight;
    private int connCount;
    private Executor executor;
    private FlowController<JRequest> flowController;

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
        checkNotNull(acceptor, "acceptor");

        JServer.ServiceRegistry registry = acceptor.getAcceptor().serviceRegistry();

        // Provider粒度拦截器
        if (providerInterceptors.length > 0) {
            registry.provider(
                    new ProviderProxyHandler().withIntercept(providerInterceptors),
                    providerImpl);
        } else {
            registry.provider(providerImpl);
        }

        serviceWrapper = registry
                .weight(weight)                     // 权重 (大于0有效)
                .connCount(connCount)               // 维持长连接数量 (大于0有效)
                .executor(executor)                 // 私有线程池
                .flowController(flowController)     // Provider粒度流控
                .register();
    }

    public JupiterSpringAcceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(JupiterSpringAcceptor acceptor) {
        this.acceptor = acceptor;
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

    public int getConnCount() {
        return connCount;
    }

    public void setConnCount(int connCount) {
        this.connCount = connCount;
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

    private final class JupiterApplicationListener implements ApplicationListener {

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof ContextRefreshedEvent) {
                // 发布服务
                acceptor.getAcceptor().publish(serviceWrapper);

                logger.info("#publish service: {}.", serviceWrapper);
            }
        }
    }
}
