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
import org.jupiter.common.util.Maps;
import org.jupiter.registry.NotifyListener;
import org.jupiter.registry.OfflineListener;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.channel.DirectoryJChannelGroup;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.load.balance.LoadBalance;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.registry.RegisterMeta.*;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class AbstractJClient implements JClient {

    // SPI
    private final RegistryService registryService = JServiceLoader.load(RegistryService.class);
    @SuppressWarnings("unchecked")
    private final LoadBalance<JChannelGroup> loadBalance = JServiceLoader.load(LoadBalance.class);

    private final DirectoryJChannelGroup directoryGroup = new DirectoryJChannelGroup();
    private final ConcurrentMap<UnresolvedAddress, JChannelGroup> addressGroups = Maps.newConcurrentHashMap();

    @Override
    public void initRegistryService(Object... args) {
        registryService.init(args);
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
    public void addChannelGroup(Directory directory, JChannelGroup group) {
        directory(directory).addIfAbsent(group);
    }

    @Override
    public boolean removeChannelGroup(Directory directory, JChannelGroup group) {
        return directory(directory).remove(group);
    }

    @Override
    public CopyOnWriteArrayList<JChannelGroup> directory(Directory directory) {
        return directoryGroup.list(directory);
    }

    @Override
    public boolean isDirectoryAvailable(Directory directory) {
        CopyOnWriteArrayList<JChannelGroup> groups = directory(directory);
        for (JChannelGroup g : groups) {
            if (!g.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JChannel select(Directory directory) {
        CopyOnWriteArrayList<JChannelGroup> groupList = directory(directory);
        JChannelGroup group = loadBalance.select(groupList);
        if (!group.isEmpty()) {
            return group.next();
        }

        for (JChannelGroup g : groupList) {
            if (!g.isEmpty()) {
                return g.next();
            }
        }

        throw new IllegalStateException("no channel");
    }

    @Override
    public Collection<RegisterMeta> lookup(Directory directory) {
        ServiceMeta serviceMeta = transform2ServiceMeta(directory);

        return registryService.lookup(serviceMeta);
    }

    @Override
    public void subscribe(Directory directory, NotifyListener listener) {
        registryService.subscribe(transform2ServiceMeta(directory), listener);
    }

    @Override
    public void offlineListening(UnresolvedAddress address, OfflineListener listener) {
        registryService.offlineListening(transform2Address(address), listener);
    }

    protected abstract JChannelGroup newChannelGroup(UnresolvedAddress address);

    private static ServiceMeta transform2ServiceMeta(Directory directory) {
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setGroup(checkNotNull(directory.getGroup(), "group"));
        serviceMeta.setVersion(checkNotNull(directory.getVersion(), "version"));
        serviceMeta.setServiceProviderName(checkNotNull(directory.getServiceProviderName(), "serviceProviderName"));

        return serviceMeta;
    }

    private static Address transform2Address(UnresolvedAddress address) {
        return new Address(address.getHost(), address.getPort());
    }
}
