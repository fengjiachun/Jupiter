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

import org.jupiter.common.util.ExceptionUtil;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.transport.JAcceptor;
import org.springframework.beans.factory.InitializingBean;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * 服务端 acceptor wrapper, 负责初始化并启动acceptor.
 *
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringServer implements InitializingBean {

    private JServer server;
    private RegistryService.RegisterType registerType;
    private JAcceptor acceptor;

    private String registryServerAddresses;             // 注册中心地址 [host1:port1,host2:port2....]
    private boolean hasRegistryServer;                  // true: 需要连接注册中心; false: IP直连方式
    private ProviderInterceptor[] providerInterceptors; // 全局拦截器
    private FlowController<JRequest> flowController;    // 全局流量控制

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        server = new DefaultServer(registerType);
        if (acceptor == null) {
            acceptor = createDefaultAcceptor();
        }
        server.withAcceptor(acceptor);

        // 注册中心
        if (Strings.isNotBlank(registryServerAddresses)) {
            server.connectToRegistryServer(registryServerAddresses);
            hasRegistryServer = true;
        }

        // 全局拦截器
        if (providerInterceptors != null && providerInterceptors.length > 0) {
            server.withGlobalInterceptors(providerInterceptors);
        }

        // 全局限流
        server.withGlobalFlowController(flowController);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                server.shutdownGracefully();
            }
        });

        try {
            server.start(false);
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }
    }

    public JServer getServer() {
        return server;
    }

    public void setServer(JServer server) {
        this.server = server;
    }

    public RegistryService.RegisterType getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = RegistryService.RegisterType.parse(registerType);
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

    public boolean isHasRegistryServer() {
        return hasRegistryServer;
    }

    public void setHasRegistryServer(boolean hasRegistryServer) {
        this.hasRegistryServer = hasRegistryServer;
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

    private JAcceptor createDefaultAcceptor() {
        JAcceptor defaultAcceptor = null;
        try {
            String className = SystemPropertyUtil
                    .get("jupiter.io.default.acceptor", "org.jupiter.transport.netty.JNettyTcpAcceptor");
            Class<?> clazz = Class.forName(className);
            defaultAcceptor = (JAcceptor) clazz.newInstance();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }
        return checkNotNull(defaultAcceptor, "default acceptor");
    }
}
