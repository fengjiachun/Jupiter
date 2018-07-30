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

package org.jupiter.rpc;

import org.jupiter.registry.Registry;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.transport.Directory;
import org.jupiter.transport.JAcceptor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * The jupiter rpc server.
 *
 * 注意 JServer 单例即可, 不要创建多个实例.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JServer extends Registry {

    /**
     * 本地服务注册.
     */
    interface ServiceRegistry {

        /**
         * 设置服务对象和拦截器, 拦截器可为空.
         */
        ServiceRegistry provider(Object serviceProvider, ProviderInterceptor... interceptors);

        /**
         * 设置服务接口类型, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
         */
        ServiceRegistry interfaceClass(Class<?> interfaceClass);

        /**
         * 设置服务组别, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
         */
        ServiceRegistry group(String group);

        /**
         * 设置服务名称, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
         */
        ServiceRegistry providerName(String providerName);

        /**
         * 设置服务版本号, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
         */
        ServiceRegistry version(String version);

        /**
         * 设置服务权重(0 < weight <= 100).
         */
        ServiceRegistry weight(int weight);

        /**
         * 设置服务提供者私有的线程池, 为了和其他服务提供者资源隔离.
         */
        ServiceRegistry executor(Executor executor);

        /**
         * 设置一个私有的流量限制器.
         */
        ServiceRegistry flowController(FlowController<JRequest> flowController);

        /**
         * 注册服务到本地容器.
         */
        ServiceWrapper register();
    }

    interface ProviderInitializer<T> {

        /**
         * 初始化指定服务提供者.
         */
        void init(T provider);
    }

    /**
     * 网络层acceptor.
     */
    JAcceptor acceptor();

    /**
     * 设置网络层acceptor.
     */
    JServer withAcceptor(JAcceptor acceptor);

    /**
     * 注册服务实例
     */
    RegistryService registryService();

    /**
     * 设置全局的拦截器, 会拦截所有的服务提供者.
     */
    void withGlobalInterceptors(ProviderInterceptor... globalInterceptors);

    /**
     * 返回已设置的全局的拦截器.
     */
    FlowController<JRequest> globalFlowController();

    /**
     * 设置全局的流量控制器.
     */
    void withGlobalFlowController(FlowController<JRequest> flowController);

    /**
     * 获取服务注册(本地)工具.
     */
    ServiceRegistry serviceRegistry();

    /**
     * 根据服务目录查找对应服务提供者.
     */
    ServiceWrapper lookupService(Directory directory);

    /**
     * 根据服务目录移除对应服务提供者.
     */
    ServiceWrapper removeService(Directory directory);

    /**
     * 本地容器注册的所有服务.
     */
    List<ServiceWrapper> allRegisteredServices();

    /**
     * 发布指定服务到注册中心.
     */
    void publish(ServiceWrapper serviceWrapper);

    /**
     * 发布指定服务列表到注册中心.
     */
    void publish(ServiceWrapper... serviceWrappers);

    /**
     * 服务提供者初始化完成后再发布服务到注册中心(延迟发布服务), 并设置服务私有的线程池来执行初始化操作.
     */
    <T> void publishWithInitializer(ServiceWrapper serviceWrapper, ProviderInitializer<T> initializer, Executor executor);

    /**
     * 发布本地所有服务到注册中心.
     */
    void publishAll();

    /**
     * 从注册中心把指定服务下线.
     */
    @SuppressWarnings("all")
    void unpublish(ServiceWrapper serviceWrapper);

    /**
     * 从注册中心把本地所有服务全部下线.
     */
    @SuppressWarnings("all")
    void unpublishAll();

    /**
     * 启动jupiter server, 以同步阻塞的方式启动.
     */
    void start() throws InterruptedException;

    /**
     * 启动jupiter server, 可通过参数指定异步/同步的方式启动.
     */
    void start(boolean sync) throws InterruptedException;

    /**
     * 优雅关闭jupiter server.
     */
    void shutdownGracefully();
}
