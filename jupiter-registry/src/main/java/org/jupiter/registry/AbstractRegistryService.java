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

package org.jupiter.registry;

import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Pair;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.registry.RegisterMeta.Address;
import static org.jupiter.registry.RegisterMeta.ServiceMeta;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public abstract class AbstractRegistryService implements RegistryService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRegistryService.class);

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
                    } catch (Throwable t) {
                        if (meta != null) {
                            logger.warn("Register [{}] fail: {}, will try again...", meta.getServiceMeta(), stackTrace(t));

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
    public void offlineListening(Address address, OfflineListener listener) {
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
        boolean needNotify = false;

        synchronized (registries) {
            Pair<Long, List<RegisterMeta>> oldData = registries.get(serviceMeta);
            if (oldData == null || (oldData.getKey() < version)) {
                registries.put(serviceMeta, new Pair<>(version, registerMetaList));
                needNotify = true;
            }
        }

        if (needNotify) {
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
                l.offline();
            }
        }
    }

    protected abstract void doSubscribe(ServiceMeta serviceMeta);

    protected abstract void doRegister(RegisterMeta meta);

    protected abstract void doUnregister(RegisterMeta meta);
}
