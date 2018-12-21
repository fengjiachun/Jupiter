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

import org.jupiter.common.util.*;
import org.jupiter.registry.*;
import org.jupiter.rpc.consumer.processor.DefaultConsumerProcessor;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.*;
import org.jupiter.transport.channel.JChannelGroup;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * Jupiter默认客户端实现.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultClient implements JClient {

    // 服务订阅(SPI)
    private final RegistryService registryService;
    private final String appName;

    private JConnector<JConnection> connector;

    public DefaultClient() {
        this(JConstants.UNKNOWN_APP_NAME, RegistryService.RegistryType.DEFAULT);
    }

    public DefaultClient(RegistryService.RegistryType registryType) {
        this(JConstants.UNKNOWN_APP_NAME, registryType);
    }

    public DefaultClient(String appName) {
        this(appName, RegistryService.RegistryType.DEFAULT);
    }

    public DefaultClient(String appName, RegistryService.RegistryType registryType) {
        this.appName = Strings.isBlank(appName) ? JConstants.UNKNOWN_APP_NAME : appName;
        registryType = registryType == null ? RegistryService.RegistryType.DEFAULT : registryType;
        registryService = JServiceLoader.load(RegistryService.class).find(registryType.getValue());
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
        if (connector.processor() == null) {
            connector.withProcessor(new DefaultConsumerProcessor());
        }
        this.connector = connector;
        return this;
    }

    @Override
    public RegistryService registryService() {
        return registryService;
    }

    @Override
    public Collection<RegisterMeta> lookup(Directory directory) {
        RegisterMeta.ServiceMeta serviceMeta = toServiceMeta(directory);
        return registryService.lookup(serviceMeta);
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass) {
        return watchConnections(interfaceClass, JConstants.DEFAULT_VERSION);
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass, String version) {
        checkNotNull(interfaceClass, "interfaceClass");
        ServiceProvider annotation = interfaceClass.getAnnotation(ServiceProvider.class);
        checkNotNull(annotation, interfaceClass + " is not a ServiceProvider interface");
        String providerName = annotation.name();
        providerName = Strings.isNotBlank(providerName) ? providerName : interfaceClass.getName();
        version = Strings.isNotBlank(version) ? version : JConstants.DEFAULT_VERSION;

        return watchConnections(new ServiceMetadata(annotation.group(), providerName, version));
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(final Directory directory) {
        JConnector.ConnectionWatcher manager = new JConnector.ConnectionWatcher() {

            private final JConnectionManager connectionManager = connector.connectionManager();

            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notifyCondition = lock.newCondition();
            // attempts to elide conditional wake-ups when the lock is uncontended.
            private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

            @Override
            public void start() {
                subscribe(directory, new NotifyListener() {

                    @Override
                    public void notify(RegisterMeta registerMeta, NotifyEvent event) {
                        UnresolvedAddress address = new UnresolvedSocketAddress(registerMeta.getHost(), registerMeta.getPort());
                        final JChannelGroup group = connector.group(address);
                        if (event == NotifyEvent.CHILD_ADDED) {
                            if (group.isAvailable()) {
                                onSucceed(group, signalNeeded.getAndSet(false));
                            } else {
                                if (group.isConnecting()) {
                                    group.onAvailable(new Runnable() {

                                        @Override
                                        public void run() {
                                            onSucceed(group, signalNeeded.getAndSet(false));
                                        }
                                    });
                                } else {
                                    group.setConnecting(true);
                                    JConnection[] connections = connectTo(address, group, registerMeta, true);
                                    final AtomicInteger countdown = new AtomicInteger(connections.length);
                                    for (JConnection c : connections) {
                                        c.operationComplete(new JConnection.OperationListener() {

                                            @Override
                                            public void complete(boolean isSuccess) {
                                                if (isSuccess) {
                                                    onSucceed(group, signalNeeded.getAndSet(false));
                                                }
                                                if (countdown.decrementAndGet() <= 0) {
                                                    group.setConnecting(false);
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                            group.putWeight(directory, registerMeta.getWeight());
                        } else if (event == NotifyEvent.CHILD_REMOVED) {
                            connector.removeChannelGroup(directory, group);
                            group.removeWeight(directory);
                            if (connector.directoryGroup().getRefCount(group) <= 0) {
                                connectionManager.cancelAutoReconnect(address);
                            }
                        }
                    }

                    private JConnection[] connectTo(final UnresolvedAddress address, final JChannelGroup group, RegisterMeta registerMeta, boolean async) {
                        int connCount = registerMeta.getConnCount(); // global value from single client
                        connCount = connCount < 1 ? 1 : connCount;

                        JConnection[] connections = new JConnection[connCount];
                        group.setCapacity(connCount);
                        for (int i = 0; i < connCount; i++) {
                            JConnection connection = connector.connect(address, async);
                            connections[i] = connection;
                            connectionManager.manage(connection);
                        }

                        offlineListening(address, new OfflineListener() {

                            @Override
                            public void offline() {
                                connectionManager.cancelAutoReconnect(address);
                                if (!group.isAvailable()) {
                                    connector.removeChannelGroup(directory, group);
                                }
                            }
                        });

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

                long remains = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

                boolean available = false;
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    signalNeeded.set(true);
                    // avoid "spurious wakeup" occurs
                    while (!(available = connector.isDirectoryAvailable(directory))) {
                        if ((remains = notifyCondition.awaitNanos(remains)) <= 0) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    ThrowUtil.throwException(e);
                } finally {
                    _look.unlock();
                }

                return available || connector.isDirectoryAvailable(directory);
            }
        };

        manager.start();

        return manager;
    }

    @Override
    public boolean awaitConnections(Class<?> interfaceClass, long timeoutMillis) {
        return awaitConnections(interfaceClass, JConstants.DEFAULT_VERSION, timeoutMillis);
    }

    @Override
    public boolean awaitConnections(Class<?> interfaceClass, String version, long timeoutMillis) {
        JConnector.ConnectionWatcher watcher = watchConnections(interfaceClass, version);
        return watcher.waitForAvailable(timeoutMillis);
    }

    @Override
    public boolean awaitConnections(Directory directory, long timeoutMillis) {
        JConnector.ConnectionWatcher watcher = watchConnections(directory);
        return watcher.waitForAvailable(timeoutMillis);
    }

    @Override
    public void subscribe(Directory directory, NotifyListener listener) {
        registryService.subscribe(toServiceMeta(directory), listener);
    }

    @Override
    public void offlineListening(UnresolvedAddress address, OfflineListener listener) {
        if (registryService instanceof AbstractRegistryService) {
            ((AbstractRegistryService) registryService).offlineListening(toAddress(address), listener);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void shutdownGracefully() {
        registryService.shutdownGracefully();
        connector.shutdownGracefully();
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        registryService.connectToRegistryServer(connectString);
    }

    // setter for spring-support
    public void setConnector(JConnector<JConnection> connector) {
        withConnector(connector);
    }

    private static RegisterMeta.ServiceMeta toServiceMeta(Directory directory) {
        RegisterMeta.ServiceMeta serviceMeta = new RegisterMeta.ServiceMeta();
        serviceMeta.setGroup(checkNotNull(directory.getGroup(), "group"));
        serviceMeta.setServiceProviderName(checkNotNull(directory.getServiceProviderName(), "serviceProviderName"));
        serviceMeta.setVersion(checkNotNull(directory.getVersion(), "version"));
        return serviceMeta;
    }

    private static RegisterMeta.Address toAddress(UnresolvedAddress address) {
        return new RegisterMeta.Address(address.getHost(), address.getPort());
    }
}
