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
    public void addGroup(Directory directory, JChannelGroup group) {
        directory(directory).addIfAbsent(group);
    }

    @Override
    public CopyOnWriteArrayList<JChannelGroup> directory(Directory directory) {
        return directoryGroup.list(directory);
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
        ServiceMeta serviceMeta = cast2ServiceMeta(directory);

        return registryService.lookup(serviceMeta);
    }

    @Override
    public void subscribe(Directory directory, NotifyListener listener) {
        registryService.subscribe(cast2ServiceMeta(directory), listener);
    }

    @Override
    public void subscribe(UnresolvedAddress address, OfflineListener listener) {
        registryService.subscribe(cast2Address(address), listener);
    }

    protected abstract JChannelGroup newChannelGroup(UnresolvedAddress address);

    private static ServiceMeta cast2ServiceMeta(Directory directory) {
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setGroup(checkNotNull(directory.getGroup(), "group"));
        serviceMeta.setVersion(checkNotNull(directory.getVersion(), "version"));
        serviceMeta.setServiceProviderName(checkNotNull(directory.getServiceProviderName(), "serviceProviderName"));

        return serviceMeta;
    }

    private static Address cast2Address(UnresolvedAddress address) {
        return new Address(address.getHost(), address.getPort());
    }
}
