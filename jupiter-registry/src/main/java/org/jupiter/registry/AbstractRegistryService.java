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

import org.jupiter.common.concurrent.ConcurrentSet;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.registry.NotifyListener.*;
import static org.jupiter.registry.NotifyListener.NotifyEvent.*;
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

    private final LinkedBlockingQueue<RegisterMeta> queue = new LinkedBlockingQueue<>(1024);
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("registry.executor"));
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private final Map<ServiceMeta, Pair<Long, List<RegisterMeta>>> registries = Maps.newHashMap();
    private final ReentrantReadWriteLock registriesLock = new ReentrantReadWriteLock();

    private final ConcurrentMap<ServiceMeta, CopyOnWriteArrayList<NotifyListener>> subscribeListeners = Maps.newConcurrentHashMap();
    private final ConcurrentMap<Address, CopyOnWriteArrayList<OfflineListener>> offlineListeners = Maps.newConcurrentHashMap();

    // Consumer已订阅的信息
    private final ConcurrentSet<ServiceMeta> subscribeSet = new ConcurrentSet<>();
    // Provider已发布的注册信息
    private final ConcurrentSet<RegisterMeta> registerMetaSet = new ConcurrentSet<>();

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
    public Collection<RegisterMeta> lookup(ServiceMeta serviceMeta) {
        Pair<Long, List<RegisterMeta>> data;

        final Lock readLock = registriesLock.readLock();
        readLock.lock();
        try {
            data = registries.get(serviceMeta);
        } finally {
            readLock.unlock();
        }

        if (data != null) {
            return Lists.newArrayList(data.getValue());
        }
        return Collections.emptyList();
    }

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

    public void offline(Address address) {
        // remove and notify
        CopyOnWriteArrayList<OfflineListener> listeners = offlineListeners.remove(address);
        if (listeners != null) {
            for (OfflineListener l : listeners) {
                l.offline();
            }
        }
    }

    public ConcurrentSet<ServiceMeta> subscribeSet() {
        return subscribeSet;
    }

    public ConcurrentSet<RegisterMeta> registerMetaSet() {
        return registerMetaSet;
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    public void shutdown() {
        if (!shutdown.getAndSet(true)) {
            executor.shutdown();
            try {
                destroy();
            } catch (Exception ignored) {}
        }
    }

    public abstract void destroy();

    // 通知新增或删除服务
    protected void notify(ServiceMeta serviceMeta, RegisterMeta registerMeta, NotifyEvent event, long version) {
        boolean notifyNeeded = false;

        final Lock writeLock = registriesLock.writeLock();
        writeLock.lock();
        try {
            Pair<Long, List<RegisterMeta>> data = registries.get(serviceMeta);
            if (data == null) {
                if (event == CHILD_REMOVED) {
                    return;
                }
                List<RegisterMeta> metaList = Lists.newArrayList(registerMeta);
                data = new Pair<>(version, metaList);
                notifyNeeded = true;
            } else {
                long oldVersion = data.getKey();
                List<RegisterMeta> metaList = data.getValue();
                if (oldVersion < version || (version < 0 && oldVersion > 0 /* version 溢出 */)) {
                    if (event == CHILD_REMOVED) {
                        metaList.remove(registerMeta);
                    } else if (event == CHILD_ADDED) {
                        metaList.add(registerMeta);
                    }
                    data = new Pair<>(version, metaList);
                    notifyNeeded = true;
                }
            }

            registries.put(serviceMeta, data);
        } finally {
            writeLock.unlock();
        }

        if (notifyNeeded) {
            CopyOnWriteArrayList<NotifyListener> listeners = subscribeListeners.get(serviceMeta);
            if (listeners != null) {
                for (NotifyListener l : listeners) {
                    l.notify(registerMeta, event);
                }
            }
        }
    }

    protected abstract void doSubscribe(ServiceMeta serviceMeta);

    protected abstract void doRegister(RegisterMeta meta);

    protected abstract void doUnregister(RegisterMeta meta);
}
