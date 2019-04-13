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

import java.util.List;

import org.jupiter.common.util.Pair;
import org.jupiter.common.util.Requires;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.ThrowUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.transport.JAcceptor;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JConfigGroup;
import org.jupiter.transport.JOption;
import org.springframework.beans.factory.InitializingBean;

/**
 * 服务端 acceptor wrapper, 负责初始化并启动acceptor.
 *
 * jupiter
 * org.jupiter.spring.support
 *
 * @author jiachun.fjc
 */
public class JupiterSpringServer implements InitializingBean {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JupiterSpringServer.class);

    private JServer server;
    private RegistryService.RegistryType registryType;
    private JAcceptor acceptor;

    private List<Pair<JOption<Object>, String>> parentNetOptions;   // 网络层配置选项
    private List<Pair<JOption<Object>, String>> childNetOptions;    // 网络层配置选项
    private String registryServerAddresses;                         // 注册中心地址 [host1:port1,host2:port2....]
    private boolean hasRegistryServer;                              // true: 需要连接注册中心; false: IP直连方式
    private ProviderInterceptor[] globalProviderInterceptors;       // 全局拦截器
    private FlowController<JRequest> globalFlowController;          // 全局流量控制

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        server = new DefaultServer(registryType);
        if (acceptor == null) {
            acceptor = createDefaultAcceptor();
        }
        server.withAcceptor(acceptor);

        // 网络层配置
        JConfigGroup configGroup = acceptor.configGroup();
        if (parentNetOptions != null && !parentNetOptions.isEmpty()) {
            JConfig parent = configGroup.parent();
            for (Pair<JOption<Object>, String> config : parentNetOptions) {
                parent.setOption(config.getFirst(), config.getSecond());
                logger.info("Setting parent net option: {}", config);
            }
        }
        if (childNetOptions != null && !childNetOptions.isEmpty()) {
            JConfig child = configGroup.child();
            for (Pair<JOption<Object>, String> config : childNetOptions) {
                child.setOption(config.getFirst(), config.getSecond());
                logger.info("Setting child net option: {}", config);
            }
        }

        // 注册中心
        if (Strings.isNotBlank(registryServerAddresses)) {
            server.connectToRegistryServer(registryServerAddresses);
            hasRegistryServer = true;
        }

        // 全局拦截器
        if (globalProviderInterceptors != null && globalProviderInterceptors.length > 0) {
            server.withGlobalInterceptors(globalProviderInterceptors);
        }

        // 全局限流
        server.withGlobalFlowController(globalFlowController);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdownGracefully()));

        try {
            server.start(false);
        } catch (Exception e) {
            ThrowUtil.throwException(e);
        }
    }

    public JServer getServer() {
        return server;
    }

    public void setServer(JServer server) {
        this.server = server;
    }

    public RegistryService.RegistryType getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = RegistryService.RegistryType.parse(registryType);
    }

    public JAcceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(JAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    public List<Pair<JOption<Object>, String>> getParentNetOptions() {
        return parentNetOptions;
    }

    public void setParentNetOptions(List<Pair<JOption<Object>, String>> parentNetOptions) {
        this.parentNetOptions = parentNetOptions;
    }

    public List<Pair<JOption<Object>, String>> getChildNetOptions() {
        return childNetOptions;
    }

    public void setChildNetOptions(List<Pair<JOption<Object>, String>> childNetOptions) {
        this.childNetOptions = childNetOptions;
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

    public ProviderInterceptor[] getGlobalProviderInterceptors() {
        return globalProviderInterceptors;
    }

    public void setGlobalProviderInterceptors(ProviderInterceptor[] globalProviderInterceptors) {
        this.globalProviderInterceptors = globalProviderInterceptors;
    }

    public FlowController<JRequest> getGlobalFlowController() {
        return globalFlowController;
    }

    public void setGlobalFlowController(FlowController<JRequest> globalFlowController) {
        this.globalFlowController = globalFlowController;
    }

    private JAcceptor createDefaultAcceptor() {
        JAcceptor defaultAcceptor = null;
        try {
            String className = SystemPropertyUtil
                    .get("jupiter.io.default.acceptor", "org.jupiter.transport.netty.JNettyTcpAcceptor");
            Class<?> clazz = Class.forName(className);
            defaultAcceptor = (JAcceptor) clazz.newInstance();
        } catch (Exception e) {
            ThrowUtil.throwException(e);
        }
        return Requires.requireNotNull(defaultAcceptor, "default acceptor");
    }
}
