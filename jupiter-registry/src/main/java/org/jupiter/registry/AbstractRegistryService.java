package org.jupiter.registry;

import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jupiter.registry.RegisterMeta.*;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public abstract class AbstractRegistryService implements RegistryService {

    private final LinkedBlockingQueue<RegisterMeta> queue = new LinkedBlockingQueue<>(1204);
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("registry.executor"));
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private final Map<ServiceMeta, Pair<Long, List<RegisterMeta>>> registries = Maps.newHashMap();
    private final ConcurrentMap<ServiceMeta, CopyOnWriteArrayList<NotifyListener>> subscribeListeners = Maps.newConcurrentHashMap();
    private final ConcurrentMap<Address, CopyOnWriteArrayList<OfflineListener>> offlineListeners = Maps.newConcurrentHashMap();

    public AbstractRegistryService() {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                while (!shutdown.get()) {
                    RegisterMeta meta = null;
                    try {
                        meta = queue.take();
                        doRegister(meta);
                    } catch (Exception e) {
                        if (meta != null) {
                            queue.add(meta);
                        }
                    }
                }
            }
        });
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    public void shutdown() {
        if (!shutdown.getAndSet(true)) {
            executor.shutdown();
        }
    }

    @Override
    public void register(RegisterMeta meta) {
        queue.add(meta);
    }

    @Override
    public void unregister(RegisterMeta meta) {
        doUnregister(meta);
    }

    @Override
    public void subscribe(ServiceMeta serviceMeta, NotifyListener listener) {
        CopyOnWriteArrayList<NotifyListener> listeners = subscribeListeners.get(serviceMeta);
        if (listeners == null) {
            CopyOnWriteArrayList<NotifyListener> newListeners = new CopyOnWriteArrayList<>();
            listeners = subscribeListeners.putIfAbsent(serviceMeta, newListeners);
            if (listeners == null) {
                listeners = newListeners;
            }
        }
        listeners.add(listener);

        doSubscribe(serviceMeta);
    }

    @Override
    public void subscribe(Address address, OfflineListener listener) {
        CopyOnWriteArrayList<OfflineListener> listeners = offlineListeners.get(address);
        if (listeners == null) {
            CopyOnWriteArrayList<OfflineListener> newListeners = new CopyOnWriteArrayList<>();
            listeners = offlineListeners.putIfAbsent(address, newListeners);
            if (listeners == null) {
                listeners = newListeners;
            }
        }
        listeners.add(listener);
    }

    @Override
    public Collection<RegisterMeta> lookup(ServiceMeta serviceMeta) {
        Pair<Long, List<RegisterMeta>> data;

        synchronized (registries) {
            data = registries.get(serviceMeta);
        }

        if (data != null) {
            return Lists.newArrayList(data.getValue());
        }
        return Collections.emptyList();
    }

    // 通知新的服务
    protected void notify(ServiceMeta serviceMeta, List<RegisterMeta> registerMetaList, long version) {
        boolean notify = false;

        synchronized (registries) {
            Pair<Long, List<RegisterMeta>> oldData = registries.get(serviceMeta);
            if (oldData == null || (oldData.getKey() < version)) {
                registries.put(serviceMeta, new Pair<>(version, registerMetaList));
                notify = true;
            }
        }

        if (notify) {
            CopyOnWriteArrayList<NotifyListener> listeners = subscribeListeners.get(serviceMeta);
            if (listeners != null) {
                for (NotifyListener l : listeners) {
                    l.notify(registerMetaList);
                }
            }
        }
    }

    // 通知对应地址的机器下线
    protected void offline(Address address) {
        CopyOnWriteArrayList<OfflineListener> listeners = offlineListeners.get(address);
        if (listeners != null) {
            for (OfflineListener l : listeners) {
                l.offline(address);
            }
        }
    }

    protected abstract void doSubscribe(ServiceMeta serviceMeta);

    protected abstract void doRegister(RegisterMeta meta);

    protected abstract void doUnregister(RegisterMeta meta);
}
