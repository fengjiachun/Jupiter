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

import org.jupiter.common.concurrent.atomic.AtomicUpdater;
import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.*;
import org.jupiter.rpc.channel.DirectoryJChannelGroup;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.load.balance.LoadBalancer;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jupiter.common.util.JConstants.UNKNOWN_APP_NAME;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.registry.RegisterMeta.Address;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;
import static org.jupiter.rpc.channel.DirectoryJChannelGroup.*;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class AbstractJClient implements JClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractJClient.class);

    private static final AtomicReferenceFieldUpdater<CopyOnWriteArrayList, Object[]> groupsUpdater
            = AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteArrayList.class, Object[].class, "array");

    // SPI
    private final RegistryService registryService = JServiceLoader.load(RegistryService.class);
    @SuppressWarnings("unchecked")
    private final LoadBalancer<JChannelGroup> loadBalancer = JServiceLoader.load(LoadBalancer.class);

    private final ConcurrentMap<UnresolvedAddress, JChannelGroup> addressGroups = Maps.newConcurrentHashMap();

    private final String appName;

    public AbstractJClient() {
        this(UNKNOWN_APP_NAME);
    }

    public AbstractJClient(String appName) {
        this.appName = appName;
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        registryService.connectToRegistryServer(connectString);
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public JChannelGroup group(UnresolvedAddress address) {
        checkNotNull(address, "address");

        JChannelGroup group = addressGroups.get(address);
        if (group == null) {
            JChannelGroup newGroup = newChannelGroup(address);
            group = addressGroups.putIfAbsent(address, newGroup);
            if (group == null) {
                group = newGroup;
            }
        }
        return group;
    }

    @Override
    public Collection<JChannelGroup> groups() {
        return addressGroups.values();
    }

    @Override
    public boolean addChannelGroup(Directory directory, JChannelGroup group) {
        boolean added = directory(directory).addIfAbsent(group);
        if (added) {
            logger.info("Added channel group: {} to {}.", group, directory.directory());
        }
        return added;
    }

    @Override
    public boolean removeChannelGroup(Directory directory, JChannelGroup group) {
        CopyOnWriteGroupList groups = directory(directory);
        boolean removed = groups.remove(group);
        if (removed) {
            logger.warn("Removed channel group: {} in directory: {}.", group, directory.directory());

            if (groups.isEmpty()) {
                DirectoryJChannelGroup.remove(directory);
            }
        }
        return removed;
    }

    @Override
    public CopyOnWriteGroupList directory(Directory directory) {
        return DirectoryJChannelGroup.list(directory);
    }

    @Override
    public boolean isDirectoryAvailable(Directory directory) {
        CopyOnWriteGroupList groups = directory(directory);
        for (JChannelGroup g : groups) {
            if (g.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JChannel select(Directory directory) {
        CopyOnWriteGroupList groups = directory(directory);
        // snapshot of groupList
        Object[] elements = groupsUpdater.get(groups);
        if (elements.length == 0) {
            if (!awaitConnections(directory, 3000)) {
                throw new IllegalStateException("no connections");
            }
            elements = groupsUpdater.get(groups);
        }

        JChannelGroup group = loadBalancer.select(elements);

        if (group.isAvailable()) {
            return group.next();
        }

        refreshConnections(directory);

        // group死期到(无可用channel), 时间超过预定限制
        long deadline = group.deadlineMillis();
        if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
            boolean removed = groups.remove(group);
            if (removed) {
                logger.warn("Removed channel group: {} in directory: {} on [select].", group, directory.directory());

                if (groups.isEmpty()) {
                    DirectoryJChannelGroup.remove(directory);
                }
            }
        }

        for (JChannelGroup g : groups) {
            if (g.isAvailable()) {
                return g.next();
            }
        }

        throw new IllegalStateException("no channel");
    }

    @Override
    public Collection<RegisterMeta> lookup(Directory directory) {
        ServiceMeta serviceMeta = transformToServiceMeta(directory);

        return registryService.lookup(serviceMeta);
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

    protected abstract JChannelGroup newChannelGroup(UnresolvedAddress address);

    private static ServiceMeta transformToServiceMeta(Directory directory) {
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setGroup(checkNotNull(directory.getGroup(), "group"));
        serviceMeta.setVersion(checkNotNull(directory.getVersion(), "version"));
        serviceMeta.setServiceProviderName(checkNotNull(directory.getServiceProviderName(), "serviceProviderName"));

        return serviceMeta;
    }

    private static Address transformToAddress(UnresolvedAddress address) {
        return new Address(address.getHost(), address.getPort());
    }
}
