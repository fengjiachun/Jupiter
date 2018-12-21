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

package org.jupiter.common.concurrent;

import net.openhft.affinity.AffinityLock;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityStrategy;
import org.jupiter.common.util.ClassUtil;
import org.jupiter.common.util.internal.InternalThread;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * This is a ThreadFactory which assigns threads based the strategies provided.
 *
 * If no strategies are provided AffinityStrategies.ANY is used.
 *
 * Jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class AffinityNamedThreadFactory implements ThreadFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AffinityNamedThreadFactory.class);

    static {
        // 检查是否存在slf4j, 使用Affinity必须显式引入slf4j依赖
        ClassUtil.checkClass("org.slf4j.Logger",
                "Class[" + AffinityNamedThreadFactory.class.getName() + "] must rely on SL4J");
    }

    private final AtomicInteger id = new AtomicInteger();
    private final String name;
    private final boolean daemon;
    private final int priority;
    private final ThreadGroup group;
    private final AffinityStrategy[] strategies;
    private AffinityLock lastAffinityLock = null;

    public AffinityNamedThreadFactory(String name, AffinityStrategy... strategies) {
        this(name, false, Thread.NORM_PRIORITY, strategies);
    }

    public AffinityNamedThreadFactory(String name, boolean daemon, AffinityStrategy... strategies) {
        this(name, daemon, Thread.NORM_PRIORITY, strategies);
    }

    public AffinityNamedThreadFactory(String name, int priority, AffinityStrategy... strategies) {
        this(name, false, priority, strategies);
    }

    public AffinityNamedThreadFactory(String name, boolean daemon, int priority, AffinityStrategy... strategies) {
        this.name = "affinity." + name + " #";
        this.daemon = daemon;
        this.priority = priority;
        SecurityManager s = System.getSecurityManager();
        group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
        this.strategies = strategies.length == 0 ? new AffinityStrategy[] { AffinityStrategies.ANY } : strategies;
    }

    @Override
    public Thread newThread(Runnable r) {
        checkNotNull(r, "runnable");

        String name2 = name + id.getAndIncrement();

        final Runnable r2 = wrapRunnable(r);

        Runnable r3 = new Runnable() {

            @Override
            public void run() {
                AffinityLock al = null;
                try {
                    al = acquireLockBasedOnLast();
                } catch (Throwable ignored) { /* defensive: ignored error on acquiring lock */ }
                try {
                    r2.run();
                } finally {
                    if (al != null) {
                        try {
                            al.release();
                        } catch (Throwable ignored) { /* defensive: ignored error on releasing lock */ }
                    }
                }
            }
        };

        Thread t = wrapThread(group, r3, name2);

        try {
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) { /* doesn't matter even if failed to set. */ }

        logger.info("Creates new {}.", t);

        return t;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }

    protected Runnable wrapRunnable(Runnable r) {
        return r; // InternalThreadLocalRunnable.wrap(r)
    }

    protected Thread wrapThread(ThreadGroup group, Runnable r, String name) {
        return new InternalThread(group, r, name);
    }

    private synchronized AffinityLock acquireLockBasedOnLast() {
        AffinityLock al = lastAffinityLock == null ? AffinityLock.acquireLock() : lastAffinityLock.acquireLock(strategies);
        if (al.cpuId() >= 0) {
            if (!al.isBound()) {
                al.bind();
            }
            lastAffinityLock = al;
        }
        return al;
    }
}
