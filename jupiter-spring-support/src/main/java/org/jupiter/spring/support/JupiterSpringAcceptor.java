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

import org.jupiter.common.util.Strings;
import org.jupiter.common.util.internal.JUnsafe;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.rpc.provider.ProviderProxyHandler;
import org.jupiter.transport.JAcceptor;
import org.springframework.beans.factory.InitializingBean;

/**
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringAcceptor implements InitializingBean {

    private static final ProviderInterceptor[] EMPTY_INTERCEPTORS = new ProviderInterceptor[0];

    private JAcceptor acceptor;
    private String registryServerAddresses;
    private ProviderInterceptor[] providerInterceptors = EMPTY_INTERCEPTORS;
    private FlowController<JRequest> flowController;

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        // 注册中心
        if (Strings.isNotBlank(registryServerAddresses)) {
            acceptor.connectToRegistryServer(registryServerAddresses);
        }

        // 全局拦截器
        if (providerInterceptors.length > 0) {
            acceptor.setGlobalProviderProxyHandler(new ProviderProxyHandler().withIntercept(providerInterceptors));
        }

        // 全局限流
        acceptor.setGlobalFlowController(flowController);

        try {
            final JAcceptor acceptor = this.acceptor;

            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    acceptor.unpublishAll();
                    acceptor.shutdownGracefully();
                }
            });

            acceptor.start(false);
        } catch (Exception e) {
            JUnsafe.throwException(e);
        }
    }

    public JAcceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(JAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    public String getRegistryServerAddresses() {
        return registryServerAddresses;
    }

    public void setRegistryServerAddresses(String registryServerAddresses) {
        this.registryServerAddresses = registryServerAddresses;
    }

    public ProviderInterceptor[] getProviderInterceptors() {
        return providerInterceptors;
    }

    public void setProviderInterceptors(ProviderInterceptor[] providerInterceptors) {
        this.providerInterceptors = providerInterceptors;
    }

    public FlowController<JRequest> getFlowController() {
        return flowController;
    }

    public void setFlowController(FlowController<JRequest> flowController) {
        this.flowController = flowController;
    }
}
