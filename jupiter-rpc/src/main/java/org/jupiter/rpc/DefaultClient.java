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

import org.jupiter.common.util.ClassInitializeUtil;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.internal.JUnsafe;
import org.jupiter.registry.*;
import org.jupiter.rpc.consumer.processor.DefaultConsumerProcessor;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.*;
import org.jupiter.transport.channel.JChannelGroup;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.JConstants.UNKNOWN_APP_NAME;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.registry.RegisterMeta.Address;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;

/**
 * Jupiter默认客户端实现.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultClient implements JClient {

    static {
        // touch off TracingUtil.<clinit>
        // because getLocalAddress() and getPid() sometimes too slow
        ClassInitializeUtil.initClass("org.jupiter.rpc.tracing.TracingUtil", 500);
    }

    // 注册服务(SPI)
    private final RegistryService registryService = JServiceLoader.loadFirst(RegistryService.class);
    private final String appName;

    private JConnector<JConnection> connector;

    public DefaultClient() {
        this(UNKNOWN_APP_NAME);
    }

    public DefaultClient(String appName) {
        this.appName = appName;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public JConnector<JConnection> connector() {
        return connector;
    }

    @Override
    public JClient withConnector(JConnector<JConnection> connector) {
        connector.withProcessor(new DefaultConsumerProcessor());
        this.connector = connector;
        return this;
    }

    @Override
    public Collection<RegisterMeta> lookup(Directory directory) {
        ServiceMeta serviceMeta = transformToServiceMeta(directory);

        return registryService.lookup(serviceMeta);
    }

    @Override
    public JConnector.ConnectionManager manageConnections(Class<?> interfaceClass) {
        return manageConnections(interfaceClass, JConstants.DEFAULT_VERSION);
    }

    @Override
    public JConnector.ConnectionManager manageConnections(Class<?> interfaceClass, String version) {
        checkNotNull(interfaceClass, "interfaceClass");
        ServiceProvider annotation = interfaceClass.getAnnotation(ServiceProvider.class);
        checkNotNull(annotation, interfaceClass + " is not a ServiceProvider interface");
        String providerName = annotation.name();
        providerName = Strings.isNotBlank(providerName) ? providerName : interfaceClass.getName();
        version = Strings.isNotBlank(version) ? version : JConstants.DEFAULT_VERSION;

        return manageConnections(new ServiceMetadata(annotation.group(), providerName, version));
    }

    @Override
    public JConnector.ConnectionManager manageConnections(final Directory directory) {
        JConnector.ConnectionManager manager = new JConnector.ConnectionManager() {

            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notifyCondition = lock.newCondition();
            // Attempts to elide conditional wake-ups when the lock is uncontended.
            private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

            @Override
            public void start() {
                subscribe(directory, new NotifyListener() {

                    @Override
                    public void notify(RegisterMeta registerMeta, NotifyEvent event) {
                        UnresolvedAddress address = new UnresolvedAddress(registerMeta.getHost(), registerMeta.getPort());
                        final JChannelGroup group = connector.group(address);
                        if (event == NotifyEvent.CHILD_ADDED) {
                            if (!group.isAvailable()) {
                                JConnection[] connections = connectTo(address, group, registerMeta, true);
                                for (JConnection c : connections) {
                                    c.operationComplete(new Runnable() {

                                        @Override
                                        public void run() {
                                            onSucceed(group, signalNeeded.getAndSet(false));
                                        }
                                    });
                                }
                            } else {
                                onSucceed(group, signalNeeded.getAndSet(false));
                            }
                        } else if (event == NotifyEvent.CHILD_REMOVED) {
                            connector.removeChannelGroup(directory, group);
                            if (connector.directoryGroup().getRefCount(group) <= 0) {
                                JConnectionManager.cancelReconnect(address); // 取消自动重连
                            }
                        }
                    }

                    private JConnection[] connectTo(final UnresolvedAddress address, final JChannelGroup group, RegisterMeta registerMeta, boolean async) {
                        int connCount = registerMeta.getConnCount();
                        connCount = connCount < 1 ? 1 : connCount;

                        JConnection[] connections = new JConnection[connCount];
                        group.setWeight(registerMeta.getWeight()); // 设置权重
                        group.setCapacity(connCount);
                        for (int i = 0; i < connCount; i++) {
                            JConnection connection = connector.connect(address, async);
                            connections[i] = connection;
                            JConnectionManager.manage(connection);

                            offlineListening(address, new OfflineListener() {

                                @Override
                                public void offline() {
                                    JConnectionManager.cancelReconnect(address); // 取消自动重连
                                    if (!group.isAvailable()) {
                                        connector.removeChannelGroup(directory, group);
                                    }
                                }
                            });
                        }

                        return connections;
                    }

                    private void onSucceed(JChannelGroup group, boolean doSignal) {
                        connector.addChannelGroup(directory, group);

                        if (doSignal) {
                            final ReentrantLock _look = lock;
                            _look.lock();
                            try {
                                notifyCondition.signalAll();
                            } finally {
                                _look.unlock();
                            }
                        }
                    }
                });
            }

            @Override
            public boolean waitForAvailable(long timeoutMillis) {
                if (connector.isDirectoryAvailable(directory)) {
                    return true;
                }

                boolean available = false;
                long start = System.nanoTime();
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    while (!connector.isDirectoryAvailable(directory)) {
                        signalNeeded.set(true);
                        notifyCondition.await(timeoutMillis, MILLISECONDS);

                        available = connector.isDirectoryAvailable(directory);
                        if (available || (System.nanoTime() - start) > MILLISECONDS.toNanos(timeoutMillis)) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    JUnsafe.throwException(e);
                } finally {
                    _look.unlock();
                }

                return available;
            }
        };

        manager.start();

        return manager;
    }

    @Override
    public boolean awaitConnections(Directory directory, long timeoutMillis) {
        JConnector.ConnectionManager manager = manageConnections(directory);
        return manager.waitForAvailable(timeoutMillis);
    }

    @Override
    public void subscribe(Directory directory, NotifyListener listener) {
        registryService.subscribe(transformToServiceMeta(directory), listener);
    }

    @Override
    public void offlineListening(UnresolvedAddress address, OfflineListener listener) {
        if (registryService instanceof AbstractRegistryService) {
            ((AbstractRegistryService) registryService).offlineListening(transformToAddress(address), listener);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void shutdownGracefully() {
        connector.shutdownGracefully();
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        registryService.connectToRegistryServer(connectString);
    }

    private static ServiceMeta transformToServiceMeta(Directory directory) {
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setGroup(checkNotNull(directory.getGroup(), "group"));
        serviceMeta.setServiceProviderName(checkNotNull(directory.getServiceProviderName(), "serviceProviderName"));
        serviceMeta.setVersion(checkNotNull(directory.getVersion(), "version"));

        return serviceMeta;
    }

    private static Address transformToAddress(UnresolvedAddress address) {
        return new Address(address.getHost(), address.getPort());
    }
}
