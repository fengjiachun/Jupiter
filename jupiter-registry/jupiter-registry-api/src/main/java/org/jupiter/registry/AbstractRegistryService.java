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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;

import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.common.concurrent.collection.ConcurrentSet;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.RegisterMeta.ServiceMeta;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public abstract class AbstractRegistryService implements RegistryService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRegistryService.class);

    private final LinkedBlockingQueue<RegisterMeta> queue = new LinkedBlockingQueue<>();
    private final ExecutorService registerExecutor =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("register.executor", true));
    private final ScheduledExecutorService registerScheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("register.schedule.executor", true));
    private final ExecutorService localRegisterWatchExecutor =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("local.register.watch.executor", true));

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private final ConcurrentMap<RegisterMeta.ServiceMeta, RegisterValue> registries =
            Maps.newConcurrentMap();

    private final ConcurrentMap<RegisterMeta.ServiceMeta, CopyOnWriteArrayList<NotifyListener>> subscribeListeners =
            Maps.newConcurrentMap();
    private final ConcurrentMap<RegisterMeta.Address, CopyOnWriteArrayList<OfflineListener>> offlineListeners =
            Maps.newConcurrentMap();

    // Consumer已订阅的信息
    private final ConcurrentSet<RegisterMeta.ServiceMeta> subscribeSet = new ConcurrentSet<>();
    // Provider已发布的注册信息
    private final ConcurrentMap<RegisterMeta, RegisterState> registerMetaMap = Maps.newConcurrentMap();

    public AbstractRegistryService() {
        registerExecutor.execute(() -> {
            while (!shutdown.get()) {
                RegisterMeta meta = null;
                try {
                    meta = queue.take();
                    registerMetaMap.put(meta, RegisterState.PREPARE);
                    doRegister(meta);
                } catch (InterruptedException e) {
                    logger.warn("[register.executor] interrupted.");
                } catch (Throwable t) {
                    if (meta != null) {
                        logger.error("Register [{}] fail: {}, will try again...", meta.getServiceMeta(), StackTraceUtil.stackTrace(t));

                        // 间隔一段时间再重新入队, 让出cpu
                        final RegisterMeta finalMeta = meta;
                        registerScheduledExecutor.schedule(() -> {
                            queue.add(finalMeta);
                        }, 1, TimeUnit.SECONDS);
                    }
                }
            }
        });

        localRegisterWatchExecutor.execute(() -> {
            while (!shutdown.get()) {
                try {
                    Thread.sleep(3000);
                    doCheckRegisterNodeStatus();
                } catch (InterruptedException e) {
                    logger.warn("[local.register.watch.executor] interrupted.");
                } catch (Throwable t) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Check register node status fail: {}, will try again...", StackTraceUtil.stackTrace(t));
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
        if (!queue.remove(meta)) {
            registerMetaMap.remove(meta);
            doUnregister(meta);
        }
    }

    @Override
    public void subscribe(RegisterMeta.ServiceMeta serviceMeta, NotifyListener listener) {
        CopyOnWriteArrayList<NotifyListener> listeners = subscribeListeners.get(serviceMeta);
        if (listeners == null) {
            CopyOnWriteArrayList<NotifyListener> newListeners = new CopyOnWriteArrayList<>();
            listeners = subscribeListeners.putIfAbsent(serviceMeta, newListeners);
            if (listeners == null) {
                listeners = newListeners;
            }
        }
        listeners.add(listener);

        subscribeSet.add(serviceMeta);
        doSubscribe(serviceMeta);
    }

    @Override
    public Collection<RegisterMeta> lookup(RegisterMeta.ServiceMeta serviceMeta) {
        RegisterValue value = registries.get(serviceMeta);

        if (value == null) {
            return Collections.emptyList();
        }

        // do not try optimistic read
        final StampedLock stampedLock = value.lock;
        final long stamp = stampedLock.readLock();
        try {
            return Lists.newArrayList(value.metaSet);
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }

    @Override
    public Map<ServiceMeta, Integer> consumers() {
        Map<ServiceMeta, Integer> result = Maps.newHashMap();
        for (Map.Entry<RegisterMeta.ServiceMeta, RegisterValue> entry : registries.entrySet()) {
            RegisterValue value = entry.getValue();
            final StampedLock stampedLock = value.lock;
            long stamp = stampedLock.tryOptimisticRead();
            int optimisticVal = value.metaSet.size();
            if (stampedLock.validate(stamp)) {
                result.put(entry.getKey(), optimisticVal);
                continue;
            }
            stamp = stampedLock.readLock();
            try {
                result.put(entry.getKey(), value.metaSet.size());
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public Map<RegisterMeta, RegisterState> providers() {
        return new HashMap<>(registerMetaMap);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public void shutdownGracefully() {
        if (!shutdown.getAndSet(true)) {
            try {
                registerExecutor.shutdownNow();
                registerScheduledExecutor.shutdownNow();
                localRegisterWatchExecutor.shutdownNow();
            } catch (Exception e) {
                logger.error("Failed to shutdown: {}.", StackTraceUtil.stackTrace(e));
            } finally {
                destroy();
            }
        }
    }

    public abstract void destroy();

    public void offlineListening(RegisterMeta.Address address, OfflineListener listener) {
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

    public void offline(RegisterMeta.Address address) {
        // remove & notify
        CopyOnWriteArrayList<OfflineListener> listeners = offlineListeners.remove(address);
        if (listeners != null) {
            for (OfflineListener l : listeners) {
                l.offline();
            }
        }
    }

    // 通知新增或删除服务
    protected void notify(
            RegisterMeta.ServiceMeta serviceMeta, NotifyListener.NotifyEvent event, long version, RegisterMeta... array) {

        if (array == null || array.length == 0) {
            return;
        }

        RegisterValue value = registries.get(serviceMeta);
        if (value == null) {
            RegisterValue newValue = new RegisterValue();
            value = registries.putIfAbsent(serviceMeta, newValue);
            if (value == null) {
                value = newValue;
            }
        }

        boolean notifyNeeded = false;

        // segment-lock
        final StampedLock stampedLock = value.lock;
        final long stamp = stampedLock.writeLock();
        try {
            long lastVersion = value.version;
            if (version > lastVersion
                    || (version < 0 && lastVersion > 0 /* version overflow */)) {
                if (event == NotifyListener.NotifyEvent.CHILD_REMOVED) {
                    for (RegisterMeta m : array) {
                        value.metaSet.remove(m);
                    }
                } else if (event == NotifyListener.NotifyEvent.CHILD_ADDED) {
                    Collections.addAll(value.metaSet, array);
                }
                value.version = version;
                notifyNeeded = true;
            }
        } finally {
            stampedLock.unlockWrite(stamp);
        }

        if (notifyNeeded) {
            CopyOnWriteArrayList<NotifyListener> listeners = subscribeListeners.get(serviceMeta);
            if (listeners != null) {
                for (NotifyListener l : listeners) {
                    for (RegisterMeta m : array) {
                        l.notify(m, event);
                    }
                }
            }
        }
    }

    protected abstract void doSubscribe(RegisterMeta.ServiceMeta serviceMeta);

    protected abstract void doRegister(RegisterMeta meta);

    protected abstract void doUnregister(RegisterMeta meta);

    protected abstract void doCheckRegisterNodeStatus();

    protected ConcurrentSet<ServiceMeta> getSubscribeSet() {
        return subscribeSet;
    }

    protected ConcurrentMap<RegisterMeta, RegisterState> getRegisterMetaMap() {
        return registerMetaMap;
    }

    protected static class RegisterValue {
        private long version = Long.MIN_VALUE;
        private final Set<RegisterMeta> metaSet = new HashSet<>();
        private final StampedLock lock = new StampedLock(); // segment-lock
    }
}
