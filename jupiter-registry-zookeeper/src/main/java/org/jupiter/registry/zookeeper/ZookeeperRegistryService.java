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

package org.jupiter.registry.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.AbstractRegistryService;
import org.jupiter.registry.RegisterMeta;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;

/**
 * Zookeeper registry service.
 *
 * jupiter
 * org.jupiter.registry.zookeeper
 *
 * @author jiachun.fjc
 */
public class ZookeeperRegistryService extends AbstractRegistryService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ZookeeperRegistryService.class);

    private final String providerHost = SystemPropertyUtil.get("jupiter.server.provider.host");

    private final int sessionTimeoutMs = SystemPropertyUtil.getInt("jupiter.registry.zookeeper.sessionTimeoutMs", 60 * 1000);
    private final int connectionTimeoutMs = SystemPropertyUtil.getInt("jupiter.registry.zookeeper.connectionTimeoutMs", 15 * 1000);

    private final ConcurrentMap<ServiceMeta, PathChildrenCache> pathChildrenCaches = Maps.newConcurrentHashMap();

    private CuratorFramework configClient;

    @Override
    protected void doSubscribe(final ServiceMeta serviceMeta) {
        PathChildrenCache childrenCache = pathChildrenCaches.get(serviceMeta);
        if (childrenCache == null) {
            String directory = String.format("/jupiter/provider/%s/%s/%s",
                    serviceMeta.getGroup(),
                    serviceMeta.getVersion(),
                    serviceMeta.getServiceProviderName());

            PathChildrenCache newChildrenCache = new PathChildrenCache(configClient, directory, false);
            childrenCache = pathChildrenCaches.putIfAbsent(serviceMeta, newChildrenCache);
            if (childrenCache == null) {
                childrenCache = newChildrenCache;

                childrenCache.getListenable().addListener(new PathChildrenCacheListener() {

                    @Override
                    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {

                        logger.info("Child event: {}", event);

                        switch (event.getType()) {
                            case CHILD_ADDED:
                                ZookeeperRegistryService.this.notify(
                                        serviceMeta, parseRegisterMeta(event.getData().getPath()), true);
                                break;
                            case CHILD_REMOVED:
                                ZookeeperRegistryService.this.notify(
                                        serviceMeta, parseRegisterMeta(event.getData().getPath()), false);
                                break;
                        }
                    }
                });

                try {
                    childrenCache.start();
                } catch (Exception e) {
                    logger.warn("Subscribe {} failed, {}.", directory, e);
                }
            }
        }
    }

    @Override
    protected void doRegister(final RegisterMeta meta) {
        checkArgument(Strings.isNotBlank(providerHost), "If use zookeeper as a config server, " +
                "\'jupiter.server.provider.host\' must be set with System.setProperty().");

        String directory = String.format("/jupiter/provider/%s/%s/%s",
                meta.getGroup(),
                meta.getVersion(),
                meta.getServiceProviderName());

        try {
            if (configClient.checkExists().forPath(directory) == null) {
                configClient.create().creatingParentsIfNeeded().forPath(directory);
            }
        } catch (Exception e) {
            logger.warn("Create parent path failed, directory: {}, {}.", directory, stackTrace(e));
        }

        try {
            meta.setHost(providerHost);

            configClient.create().withMode(CreateMode.EPHEMERAL).inBackground(new BackgroundCallback() {

                @Override
                public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                    registerMetaSet().add(meta);

                    logger.info("Register: {}.", meta);
                }
            }).forPath(
                    String.format("%s/%s:%s:%s:%s",
                            directory,
                            meta.getHost(),
                            String.valueOf(meta.getPort()),
                            String.valueOf(meta.getWeight()),
                            String.valueOf(meta.getNumOfConnections())));
        } catch (Exception e) {
            logger.warn("Create register meta: {} path failed.", meta, stackTrace(e));
        }
    }

    @Override
    protected void doUnregister(final RegisterMeta meta) {
        checkArgument(Strings.isNotBlank(providerHost), "If use zookeeper as a config server, " +
                "\'jupiter.server.provider.host\' must be set with System.setProperty().");

        String directory = String.format("/jupiter/provider/%s/%s/%s",
                meta.getGroup(),
                meta.getVersion(),
                meta.getServiceProviderName());

        try {
            if (configClient.checkExists().forPath(directory) == null) {
                return;
            }
        } catch (Exception e) {
            logger.warn("Check exists with parent path failed, directory: {}, {}.", directory, stackTrace(e));
        }

        try {
            meta.setHost(providerHost);

            configClient.delete().inBackground(new BackgroundCallback() {

                @Override
                public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                    registerMetaSet().remove(meta);

                    logger.info("Unregister: {}.", meta);
                }
            }).forPath(
                    String.format("%s/%s:%s:%s:%s",
                            directory,
                            meta.getHost(),
                            String.valueOf(meta.getPort()),
                            String.valueOf(meta.getWeight()),
                            String.valueOf(meta.getNumOfConnections())));
        } catch (Exception e) {
            logger.warn("Delete register meta: {} path failed.", meta, stackTrace(e));
        }
    }

    @Override
    public void connectToConfigServer(String connectString) {
        checkNotNull(connectString, "connectString");

        configClient = CuratorFrameworkFactory.newClient(
                connectString, sessionTimeoutMs, connectionTimeoutMs, new ExponentialBackoffRetry(500, 20));

        configClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {

            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {

                logger.info("Zookeeper connection state changed {}.", newState);

                if (newState == ConnectionState.RECONNECTED) {

                    logger.info("Zookeeper connection has been re-established, will re-subscribe and re-register.");

                    // 重新订阅
                    for (ServiceMeta serviceMeta : subscribeSet()) {
                        doSubscribe(serviceMeta);
                    }

                    // 重新发布服务
                    for (RegisterMeta meta : registerMetaSet()) {
                        doRegister(meta);
                    }
                }
            }
        });

        configClient.start();
    }

    @Override
    public void destroy() {
        for (PathChildrenCache childrenCache : pathChildrenCaches.values()) {
            try {
                childrenCache.close();
            } catch (IOException ignored) {}
        }

        configClient.close();
    }

    private static RegisterMeta parseRegisterMeta(String data) {
        String[] array_0 = Strings.split(data, '/');
        RegisterMeta meta = new RegisterMeta();
        meta.setGroup(array_0[2]);
        meta.setVersion(array_0[3]);
        meta.setServiceProviderName(array_0[4]);

        String[] array_1 = Strings.split(array_0[5], ':');
        meta.setHost(array_1[0]);
        meta.setPort(Integer.parseInt(array_1[1]));
        meta.setWeight(Integer.parseInt(array_1[2]));
        meta.setNumOfConnections(Integer.parseInt(array_1[3]));

        return meta;
    }
}
