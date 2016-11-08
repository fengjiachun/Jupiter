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

import net.bytebuddy.ByteBuddy;
import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.common.util.*;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.ProviderProxyHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.Reflects.*;
import static org.jupiter.common.util.StackTraceUtil.*;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class AbstractJServer implements JServer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractJServer.class);

    // 服务延迟初始化的默认线程池
    private final Executor defaultInitializerExecutor =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("initializer"));

    // provider本地容器
    private final ServiceProviderContainer providerContainer = new DefaultServiceProviderContainer();
    // 注册服务(SPI)
    private final RegistryService registryService = JServiceLoader.loadFirst(RegistryService.class);

    // 全局拦截代理
    private volatile ProviderProxyHandler globalProviderProxyHandler;
    // 全局流量控制
    private volatile FlowController<JRequest> globalFlowController;

    @Override
    public void connectToRegistryServer(String connectString) {
        registryService.connectToRegistryServer(connectString);
    }

    @Override
    public ProviderProxyHandler getGlobalProviderProxyHandler() {
        return globalProviderProxyHandler;
    }

    @Override
    public void setGlobalProviderProxyHandler(ProviderProxyHandler globalProviderProxyHandler) {
        this.globalProviderProxyHandler = globalProviderProxyHandler;
    }

    @Override
    public FlowController<JRequest> getGlobalFlowController() {
        return globalFlowController;
    }

    @Override
    public void setGlobalFlowController(FlowController<JRequest> globalFlowController) {
        this.globalFlowController = globalFlowController;
    }

    @Override
    public ServiceRegistry serviceRegistry() {
        return new DefaultServiceRegistry();
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return providerContainer.lookupService(directory.directory());
    }

    @Override
    public ServiceWrapper removeService(Directory directory) {
        return providerContainer.removeService(directory.directory());
    }

    @Override
    public List<ServiceWrapper> getRegisteredServices() {
        return providerContainer.getAllServices();
    }

    @Override
    public void publish(ServiceWrapper serviceWrapper) {
        ServiceMetadata metadata = serviceWrapper.getMetadata();

        RegisterMeta meta = new RegisterMeta();
        meta.setPort(bindPort());
        meta.setGroup(metadata.getGroup());
        meta.setVersion(metadata.getVersion());
        meta.setServiceProviderName(metadata.getServiceProviderName());
        meta.setWeight(serviceWrapper.getWeight());
        meta.setConnCount(serviceWrapper.getConnCount());

        registryService.register(meta);
    }

    @Override
    public void publish(ServiceWrapper... serviceWrappers) {
        for (ServiceWrapper wrapper : serviceWrappers) {
            publish(wrapper);
        }
    }

    @Override
    public <T> void publishWithInitializer(ServiceWrapper serviceWrapper, ProviderInitializer<T> initializer) {
        publishWithInitializer(serviceWrapper, initializer, null);
    }

    @Override
    public <T> void publishWithInitializer(
            final ServiceWrapper serviceWrapper, final ProviderInitializer<T> initializer, Executor executor) {
        Runnable task = new Runnable() {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    initializer.init((T) serviceWrapper.getServiceProvider());
                    publish(serviceWrapper);
                } catch (Exception e) {
                    logger.error("Error on {} #publishWithInitializer: {}.", serviceWrapper.getMetadata(), stackTrace(e));
                }
            }
        };
        if (executor == null) {
            defaultInitializerExecutor.execute(task);
        } else {
            executor.execute(task);
        }
    }

    @Override
    public void publishAll() {
        for (ServiceWrapper wrapper : providerContainer.getAllServices()) {
            publish(wrapper);
        }
    }

    @Override
    public void unpublish(ServiceWrapper serviceWrapper) {
        ServiceMetadata metadata = serviceWrapper.getMetadata();

        RegisterMeta meta = new RegisterMeta();
        meta.setPort(bindPort());
        meta.setGroup(metadata.getGroup());
        meta.setVersion(metadata.getVersion());
        meta.setServiceProviderName(metadata.getServiceProviderName());
        meta.setWeight(serviceWrapper.getWeight());
        meta.setConnCount(serviceWrapper.getConnCount());

        registryService.unregister(meta);
    }

    @Override
    public void unpublishAll() {
        for (ServiceWrapper wrapper : providerContainer.getAllServices()) {
            unpublish(wrapper);
        }
    }

    protected abstract int bindPort();

    ServiceWrapper registerService(
            String group,
            String version,
            String providerName,
            Object serviceProvider,
            Map<String, List<Class<?>[]>> methodsParameterTypes,
            int weight,
            int connCount,
            Executor executor,
            FlowController<JRequest> flowController) {

        ServiceWrapper wrapper = new ServiceWrapper(
                group, version, providerName, serviceProvider, methodsParameterTypes);
        wrapper.setWeight(weight);
        wrapper.setConnCount(connCount);
        wrapper.setExecutor(executor);
        wrapper.setFlowController(flowController);

        providerContainer.registerService(wrapper.getMetadata().directory(), wrapper);

        return wrapper;
    }

    // 生成provider代理类
    private static <T> Class<? extends T> generateProviderProxyClass(ProviderProxyHandler proxyHandler, Class<T> providerCls) {
        checkNotNull(proxyHandler, "ProviderProxyHandler");

        try {
            return new ByteBuddy()
                    .subclass(providerCls)
                    .method(isDeclaredBy(providerCls))
                    .intercept(to(proxyHandler, "handler").filter(not(isDeclaredBy(Object.class))))
                    .make()
                    .load(providerCls.getClassLoader(), INJECTION)
                    .getLoaded();
        } catch (Exception e) {
            logger.error("Generate proxy [{}, handler: {}] fail: {}.", providerCls, proxyHandler, stackTrace(e));

            return providerCls;
        }
    }

    private static <F, T> T copyProviderProperties(F provider, T proxy) {
        checkNotNull(provider, "provider");
        checkNotNull(proxy, "proxy");

        List<String> providerFieldNames = Lists.newArrayList();
        for (Class<?> cls = provider.getClass(); cls != null; cls = cls.getSuperclass()) {
            try {
                for (Field f : cls.getDeclaredFields()) {
                    providerFieldNames.add(f.getName());
                }
            } catch (Throwable ignored) {}
        }

        for (String name : providerFieldNames) {
            try {
                setValue(proxy, name, getValue(provider, name));
            } catch (Throwable ignored) {}
        }
        return proxy;
    }

    class DefaultServiceRegistry implements ServiceRegistry {

        private Object serviceProvider;                     // 服务对象
        private int weight;                                 // 权重
        private int connCount;                              // 建议客户端维持的长连接数量
        protected Executor executor;                        // 该服务私有的线程池
        protected FlowController<JRequest> flowController;  // 该服务私有的流量控制器

        @Override
        public ServiceRegistry provider(Object serviceProvider) {
            if (globalProviderProxyHandler == null) {
                this.serviceProvider = serviceProvider;
            } else {
                Class<?> globalProxyCls = generateProviderProxyClass(globalProviderProxyHandler, serviceProvider.getClass());
                this.serviceProvider = copyProviderProperties(serviceProvider, newInstance(globalProxyCls));
            }
            return this;
        }

        @Override
        public ServiceRegistry provider(ProviderProxyHandler proxyHandler, Object serviceProvider) {
            Class<?> proxyCls = generateProviderProxyClass(proxyHandler, serviceProvider.getClass());
            if (globalProviderProxyHandler == null) {
                this.serviceProvider = copyProviderProperties(serviceProvider, newInstance(proxyCls));
            } else {
                Class<?> globalProxyCls = generateProviderProxyClass(globalProviderProxyHandler, proxyCls);
                this.serviceProvider = copyProviderProperties(serviceProvider, newInstance(globalProxyCls));
            }
            return this;
        }

        @Override
        public ServiceRegistry weight(int weight) {
            this.weight = weight;
            return this;
        }

        @Override
        public ServiceRegistry connCount(int connCount) {
            this.connCount = connCount;
            return this;
        }

        @Override
        public ServiceRegistry executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public ServiceRegistry flowController(FlowController<JRequest> flowController) {
            this.flowController = flowController;
            return this;
        }

        @Override
        public ServiceWrapper register() {
            checkNotNull(serviceProvider, "serviceProvider");

            ServiceProvider annotation = null;
            String providerName = null;
            Map<String, List<Class<?>[]>> methodsParameterTypes = Maps.newHashMap();
            for (Class<?> cls = serviceProvider.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
                Class<?>[] interfaces = cls.getInterfaces();
                if (interfaces != null) {
                    for (Class<?> providerInterface : interfaces) {
                        annotation = providerInterface.getAnnotation(ServiceProvider.class);
                        if (annotation == null) {
                            continue;
                        }

                        providerName = annotation.value();
                        providerName = Strings.isNotBlank(providerName) ? providerName : providerInterface.getSimpleName();

                        // method's parameterTypes
                        for (Method method : providerInterface.getMethods()) {
                            String methodName = method.getName();
                            List<Class<?>[]> list = methodsParameterTypes.get(methodName);
                            if (list == null) {
                                list = Lists.newArrayList();
                                methodsParameterTypes.put(methodName, list);
                            }
                            list.add(method.getParameterTypes());
                        }
                        break;
                    }
                }
                if (annotation != null) {
                    break;
                }
            }

            checkArgument(annotation != null, serviceProvider.getClass() + " is not a ServiceProvider");

            String group = annotation.group();
            String version = annotation.version();

            checkNotNull(group, "group");
            checkNotNull(version, "version");

            return registerService(
                    group,
                    version,
                    providerName,
                    serviceProvider,
                    methodsParameterTypes,
                    weight,
                    connCount,
                    executor,
                    flowController
            );
        }
    }

    /**
     * Local service provider container.
     *
     * 本地provider容器
     */
    interface ServiceProviderContainer {

        /**
         * 注册服务(注意并不是发布服务到注册中心, 只是注册到本地容器)
         */
        void registerService(String uniqueKey, ServiceWrapper serviceWrapper);

        /**
         * 本地容器查找服务
         */
        ServiceWrapper lookupService(String uniqueKey);

        /**
         * 从本地容器移除服务
         */
        ServiceWrapper removeService(String uniqueKey);

        /**
         * 获取本地容器中所有服务
         */
        List<ServiceWrapper> getAllServices();
    }

    // 本地provider容器默认实现
    class DefaultServiceProviderContainer implements ServiceProviderContainer {

        private final ConcurrentMap<String, ServiceWrapper> serviceProviders = Maps.newConcurrentHashMap();

        @Override
        public void registerService(String uniqueKey, ServiceWrapper serviceWrapper) {
            serviceProviders.put(uniqueKey, serviceWrapper);

            logger.debug("ServiceProvider [{}, {}] is registered.", uniqueKey, serviceWrapper.getServiceProvider());
        }

        @Override
        public ServiceWrapper lookupService(String uniqueKey) {
            return serviceProviders.get(uniqueKey);
        }

        @Override
        public ServiceWrapper removeService(String uniqueKey) {
            ServiceWrapper provider = serviceProviders.remove(uniqueKey);
            if (provider == null) {
                logger.warn("ServiceProvider [{}] not found.", uniqueKey);
            } else {
                logger.debug("ServiceProvider [{}, {}] is removed.", uniqueKey, provider.getServiceProvider());
            }
            return provider;
        }

        @Override
        public List<ServiceWrapper> getAllServices() {
            return Lists.newArrayList(serviceProviders.values());
        }
    }
}
