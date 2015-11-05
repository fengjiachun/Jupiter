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
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.registry.RegisterMeta.Address;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class AbstractJClient implements JClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractJClient.class);

    // SPI
    private final RegistryService registryService = JServiceLoader.load(RegistryService.class);
    @SuppressWarnings("unchecked")
    private final LoadBalance<JChannelGroup> loadBalance = JServiceLoader.load(LoadBalance.class);

    private final DirectoryJChannelGroup directoryGroup = new DirectoryJChannelGroup();
    private final ConcurrentMap<UnresolvedAddress, JChannelGroup> addressGroups = Maps.newConcurrentHashMap();

    private final long lossTimeMinutesLimit = SystemPropertyUtil.getLong("jupiter.channel.group.loss.time.minutes.limit", 5);

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
    public boolean addChannelGroup(Directory directory, JChannelGroup group) {
        boolean added = directory(directory).addIfAbsent(group);
        if (added) {
            logger.info("Added channel group: {} to {}.", group, directory.directory());
        }
        return added;
    }

    @Override
    public boolean removeChannelGroup(Directory directory, JChannelGroup group) {
        boolean removed = directory(directory).remove(group);
        if (removed) {
            logger.warn("Removed channel group: {} in directory: {}.", group, directory.directory());
        }
        return removed;
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

        // group死亡时间(无可用channel)超过限制
        long lossTime = group.getLossTimestamp();
        if (lossTime > 0 &&
                MILLISECONDS.toMinutes(SystemClock.millisClock().now() - lossTime) > lossTimeMinutesLimit) {
            boolean removed = groupList.remove(group);
            if (removed) {
                logger.warn("Removed channel group: {} in directory: {}.", group, directory.directory());
            }
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
