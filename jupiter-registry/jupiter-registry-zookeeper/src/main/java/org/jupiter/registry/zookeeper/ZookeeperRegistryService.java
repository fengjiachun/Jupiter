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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.jupiter.common.concurrent.collection.ConcurrentSet;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.NetUtil;
import org.jupiter.common.util.SpiMetadata;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.AbstractRegistryService;
import org.jupiter.registry.NotifyListener;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.RegisterMeta.Address;

import static org.apache.zookeeper.KeeperException.Code;
import static org.jupiter.common.util.Requires.requireNotNull;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * Zookeeper registry service.
 *
 * jupiter
 * org.jupiter.registry.zookeeper
 *
 * @author jiachun.fjc
 */
@SpiMetadata(name = "zookeeper")
public class ZookeeperRegistryService extends AbstractRegistryService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ZookeeperRegistryService.class);

    // 没有实际意义, 不要在意它
    private static final AtomicLong sequence = new AtomicLong(0);

    private final String address = SystemPropertyUtil.get("jupiter.local.address", NetUtil.getLocalAddress());

    private final int sessionTimeoutMs = SystemPropertyUtil.getInt("jupiter.registry.zookeeper.sessionTimeoutMs", 60 * 1000);
    private final int connectionTimeoutMs = SystemPropertyUtil.getInt("jupiter.registry.zookeeper.connectionTimeoutMs", 15 * 1000);

    private final ConcurrentMap<RegisterMeta.ServiceMeta, PathChildrenCache> pathChildrenCaches = Maps.newConcurrentMap();
    // 指定节点都提供了哪些服务
    private final ConcurrentMap<Address, ConcurrentSet<RegisterMeta.ServiceMeta>> serviceMetaMap = Maps.newConcurrentMap();

    private CuratorFramework configClient;

    @Override
    public Collection<RegisterMeta> lookup(RegisterMeta.ServiceMeta serviceMeta) {
        String directory = String.format("/jupiter/provider/%s/%s/%s",
                serviceMeta.getGroup(),
                serviceMeta.getServiceProviderName(),
                serviceMeta.getVersion());

        List<RegisterMeta> registerMetaList = Lists.newArrayList();
        try {
            List<String> paths = configClient.getChildren().forPath(directory);
            for (String p : paths) {
                registerMetaList.add(parseRegisterMeta(String.format("%s/%s", directory, p)));
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Lookup service meta: {} path failed, {}.", serviceMeta, stackTrace(e));
            }
        }
        return registerMetaList;
    }

    @Override
    protected void doSubscribe(final RegisterMeta.ServiceMeta serviceMeta) {
        PathChildrenCache childrenCache = pathChildrenCaches.get(serviceMeta);
        if (childrenCache == null) {
            String directory = String.format("/jupiter/provider/%s/%s/%s",
                    serviceMeta.getGroup(),
                    serviceMeta.getServiceProviderName(),
                    serviceMeta.getVersion());

            PathChildrenCache newChildrenCache = new PathChildrenCache(configClient, directory, false);
            childrenCache = pathChildrenCaches.putIfAbsent(serviceMeta, newChildrenCache);
            if (childrenCache == null) {
                childrenCache = newChildrenCache;

                childrenCache.getListenable().addListener((client, event) -> {

                    logger.info("Child event: {}", event);

                    switch (event.getType()) {
                        case CHILD_ADDED: {
                            RegisterMeta registerMeta = parseRegisterMeta(event.getData().getPath());
                            Address address = registerMeta.getAddress();
                            RegisterMeta.ServiceMeta serviceMeta1 = registerMeta.getServiceMeta();
                            ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = getServiceMeta(address);

                            serviceMetaSet.add(serviceMeta1);
                            ZookeeperRegistryService.super.notify(
                                    serviceMeta1,
                                    NotifyListener.NotifyEvent.CHILD_ADDED,
                                    sequence.incrementAndGet(),
                                    registerMeta);

                            break;
                        }
                        case CHILD_REMOVED: {
                            RegisterMeta registerMeta = parseRegisterMeta(event.getData().getPath());
                            Address address = registerMeta.getAddress();
                            RegisterMeta.ServiceMeta serviceMeta1 = registerMeta.getServiceMeta();
                            ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = getServiceMeta(address);

                            serviceMetaSet.remove(serviceMeta1);
                            ZookeeperRegistryService.super.notify(
                                    serviceMeta1,
                                    NotifyListener.NotifyEvent.CHILD_REMOVED,
                                    sequence.incrementAndGet(),
                                    registerMeta);

                            if (serviceMetaSet.isEmpty()) {
                                logger.info("Offline notify: {}.", address);

                                ZookeeperRegistryService.super.offline(address);
                            }
                            break;
                        }
                    }
                });

                try {
                    childrenCache.start();
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Subscribe {} failed, {}.", directory, stackTrace(e));
                    }
                }
            } else {
                try {
                    newChildrenCache.close();
                } catch (IOException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Close [PathChildrenCache] {} failed, {}.", directory, stackTrace(e));
                    }
                }
            }
        }
    }

    @Override
    protected void doRegister(final RegisterMeta meta) {
        String directory = String.format("/jupiter/provider/%s/%s/%s",
                meta.getGroup(),
                meta.getServiceProviderName(),
                meta.getVersion());

        try {
            if (configClient.checkExists().forPath(directory) == null) {
                configClient.create().creatingParentsIfNeeded().forPath(directory);
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Create parent path failed, directory: {}, {}.", directory, stackTrace(e));
            }
        }

        try {
            meta.setHost(address);

            // The znode will be deleted upon the client's disconnect.
            configClient.create().withMode(CreateMode.EPHEMERAL).inBackground((client, event) -> {
                if (event.getResultCode() == Code.OK.intValue()) {
                    getRegisterMetaMap().put(meta, RegisterState.DONE);
                }

                logger.info("Register: {} - {}.", meta, event);
            }).forPath(
                    String.format("%s/%s:%s:%s:%s",
                            directory,
                            meta.getHost(),
                            String.valueOf(meta.getPort()),
                            String.valueOf(meta.getWeight()),
                            String.valueOf(meta.getConnCount())));
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Create register meta: {} path failed, {}.", meta, stackTrace(e));
            }
        }
    }

    @SuppressWarnings("all")
    @Override
    protected void doUnregister(final RegisterMeta meta) {
        String directory = String.format("/jupiter/provider/%s/%s/%s",
                meta.getGroup(),
                meta.getServiceProviderName(),
                meta.getVersion());

        try {
            if (configClient.checkExists().forPath(directory) == null) {
                return;
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Check exists with parent path failed, directory: {}, {}.", directory, stackTrace(e));
            }
        }

        try {
            meta.setHost(address);

            configClient.delete().inBackground(new BackgroundCallback() {

                @Override
                public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                    logger.info("Unregister: {} - {}.", meta, event);
                }
            }).forPath(
                    String.format("%s/%s:%s:%s:%s",
                            directory,
                            meta.getHost(),
                            String.valueOf(meta.getPort()),
                            String.valueOf(meta.getWeight()),
                            String.valueOf(meta.getConnCount())));
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Delete register meta: {} path failed, {}.", meta, stackTrace(e));
            }
        }
    }

    @Override
    protected void doCheckRegisterNodeStatus() {
        for (Map.Entry<RegisterMeta, RegisterState> entry : getRegisterMetaMap().entrySet()) {
            if (entry.getValue() == RegisterState.DONE) {
                continue;
            }

            RegisterMeta meta = entry.getKey();
            String directory = String.format("/jupiter/provider/%s/%s/%s",
                    meta.getGroup(),
                    meta.getServiceProviderName(),
                    meta.getVersion());

            String nodePath = String.format("%s/%s:%s:%s:%s",
                    directory,
                    meta.getHost(),
                    String.valueOf(meta.getPort()),
                    String.valueOf(meta.getWeight()),
                    String.valueOf(meta.getConnCount()));

            try {
                if (configClient.checkExists().forPath(nodePath) == null) {
                    super.register(meta);
                }
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Check register status, meta: {} path failed, {}.", meta, stackTrace(e));
                }
            }
        }
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        requireNotNull(connectString, "connectString");

        configClient = CuratorFrameworkFactory.newClient(
                connectString, sessionTimeoutMs, connectionTimeoutMs, new ExponentialBackoffRetry(500, 20));

        configClient.getConnectionStateListenable().addListener((client, newState) -> {

            logger.info("Zookeeper connection state changed {}.", newState);

            if (newState == ConnectionState.RECONNECTED) {

                logger.info("Zookeeper connection has been re-established, will re-subscribe and re-register.");

                // 重新订阅
                for (RegisterMeta.ServiceMeta serviceMeta : getSubscribeSet()) {
                    doSubscribe(serviceMeta);
                }

                // 重新发布服务
                for (RegisterMeta meta : getRegisterMetaMap().keySet()) {
                    ZookeeperRegistryService.super.register(meta);
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

    public List<RegisterMeta.ServiceMeta> findServiceMetaByAddress(Address address) {
        return Lists.transform(
                Lists.newArrayList(getServiceMeta(address)),
                input -> {
                    RegisterMeta.ServiceMeta copy = new RegisterMeta.ServiceMeta();
                    copy.setGroup(input.getGroup());
                    copy.setServiceProviderName(input.getServiceProviderName());
                    copy.setVersion(input.getVersion());
                    return copy;
                });
    }

    private RegisterMeta parseRegisterMeta(String data) {
        String[] array_0 = Strings.split(data, '/');
        RegisterMeta meta = new RegisterMeta();
        meta.setGroup(array_0[2]);
        meta.setServiceProviderName(array_0[3]);
        meta.setVersion(array_0[4]);

        String[] array_1 = Strings.split(array_0[5], ':');
        meta.setHost(array_1[0]);
        meta.setPort(Integer.parseInt(array_1[1]));
        meta.setWeight(Integer.parseInt(array_1[2]));
        meta.setConnCount(Integer.parseInt(array_1[3]));

        return meta;
    }

    private ConcurrentSet<RegisterMeta.ServiceMeta> getServiceMeta(Address address) {
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = serviceMetaMap.get(address);
        if (serviceMetaSet == null) {
            ConcurrentSet<RegisterMeta.ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
            serviceMetaSet = serviceMetaMap.putIfAbsent(address, newServiceMetaSet);
            if (serviceMetaSet == null) {
                serviceMetaSet = newServiceMetaSet;
            }
        }
        return serviceMetaSet;
    }
}
