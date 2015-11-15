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

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.annotation.ServiceProvider;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.limiter.TpsLimiter;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.DEFAULT_WEIGHT;
import static org.jupiter.common.util.JConstants.DEFAULT_NUM_CONNECTIONS;
import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class AbstractJServer implements JServer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractJServer.class);

    private final ServiceProviderContainer providerContainer = new DefaultServiceProviderContainer();
    // SPI
    private final RegistryService registryService = JServiceLoader.load(RegistryService.class);

    private volatile TpsLimiter<JRequest> tpsLimiter;

    @Override
    public void connectToConfigServer(String host, int port) {
        registryService.connectToConfigServer(host, port);
    }

    @Override
    public TpsLimiter<JRequest> getTpsLimiter() {
        return tpsLimiter;
    }

    @Override
    public void setTpsLimiter(TpsLimiter<JRequest> tpsLimiter) {
        this.tpsLimiter = tpsLimiter;
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
        publish(serviceWrapper, -1, -1);
    }

    @Override
    public void publish(ServiceWrapper serviceWrapper, int weight, int numOfConnections) {
        ServiceMetadata metadata = serviceWrapper.getMetadata();

        RegisterMeta meta = new RegisterMeta();
        meta.setPort(bindPort());
        meta.setGroup(metadata.getGroup());
        meta.setVersion(metadata.getVersion());
        meta.setServiceProviderName(metadata.getServiceProviderName());
        meta.setWeight(weight <= 0 ? DEFAULT_WEIGHT : weight);
        meta.setNumOfConnections(numOfConnections <= 0 ? DEFAULT_NUM_CONNECTIONS : numOfConnections);

        registryService.register(meta);
    }

    @Override
    public void publishAll() {
        publishAll(-1, -1);
    }

    @Override
    public void publishAll(int weight, int numOfConnections) {
        for (ServiceWrapper wrapper : providerContainer.getAllServices()) {
            publish(wrapper, weight, numOfConnections);
        }
    }

    protected abstract int bindPort();

    ServiceWrapper registerService(String group, String version, String name, Object serviceProvider,
            Executor executor, TpsLimiter<JRequest> tpsLimiter) {

        ServiceWrapper serviceWrapper = new ServiceWrapper(group, version, name, serviceProvider);
        serviceWrapper.setExecutor(executor);
        serviceWrapper.setTpsLimiter(tpsLimiter);

        providerContainer.registerService(serviceWrapper.getMetadata().directory(), serviceWrapper);

        return serviceWrapper;
    }

    class DefaultServiceRegistry implements ServiceRegistry {

        private Object serviceProvider;
        protected Executor executor;
        protected TpsLimiter<JRequest> tpsLimiter;

        @Override
        public ServiceRegistry provider(Object serviceProvider) {
            this.serviceProvider = serviceProvider;
            return this;
        }

        @Override
        public ServiceRegistry executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public ServiceRegistry tpsLimiter(TpsLimiter<JRequest> tpsLimiter) {
            this.tpsLimiter = tpsLimiter;
            return this;
        }

        @Override
        public ServiceWrapper register() {
            checkNotNull(serviceProvider, "serviceProvider");

            Class<?>[] interfaces = serviceProvider.getClass().getInterfaces();
            ServiceProvider annotation = null;
            String name = null;
            if (interfaces != null) {
                for (Class<?> clazz : interfaces) {
                    annotation = clazz.getAnnotation(ServiceProvider.class);
                    if (annotation == null) {
                        continue;
                    }

                    name = annotation.value();
                    name = Strings.isNotBlank(name) ? name : clazz.getSimpleName();
                    break;
                }
            }
            checkArgument(annotation != null, serviceProvider.getClass() + " is not a ServiceProvider");

            String group = annotation.group();
            String version = annotation.version();

            checkNotNull(group, "group");
            checkNotNull(version, "version");

            return registerService(group, version, name, serviceProvider, executor, tpsLimiter);
        }
    }

    /**
     * Local service provider container.
     */
    interface ServiceProviderContainer {

        void registerService(String uniqueKey, ServiceWrapper serviceWrapper);

        ServiceWrapper lookupService(String uniqueKey);

        ServiceWrapper removeService(String uniqueKey);

        List<ServiceWrapper> getAllServices();
    }

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
