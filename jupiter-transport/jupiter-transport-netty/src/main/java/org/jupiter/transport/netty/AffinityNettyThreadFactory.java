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

package org.jupiter.transport.netty;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;
import net.openhft.affinity.AffinityLock;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityStrategy;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a ThreadFactory which assigns threads based the strategies provided.
 * <p>
 * If no strategies are provided AffinityStrategies.ANY is used.
 *
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class AffinityNettyThreadFactory implements ThreadFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AffinityNettyThreadFactory.class);

    private final AtomicInteger id = new AtomicInteger();
    private final String name;
    private final boolean daemon;
    private final int priority;
    private final ThreadGroup group;
    private final AffinityStrategy[] strategies;
    private AffinityLock lastAffinityLock = null;

    public AffinityNettyThreadFactory(String name, AffinityStrategy... strategies) {
        this(name, false, Thread.NORM_PRIORITY, strategies);
    }

    public AffinityNettyThreadFactory(String name, boolean daemon, AffinityStrategy... strategies) {
        this(name, daemon, Thread.NORM_PRIORITY, strategies);
    }

    public AffinityNettyThreadFactory(String name, int priority, AffinityStrategy... strategies) {
        this(name, false, priority, strategies);
    }

    public AffinityNettyThreadFactory(String name, boolean daemon, int priority, AffinityStrategy... strategies) {
        this.name = "affinity." + name + " #";
        this.daemon = daemon;
        this.priority = priority;
        SecurityManager s = System.getSecurityManager();
        group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
        this.strategies = strategies.length == 0 ? new AffinityStrategy[] { AffinityStrategies.ANY } : strategies;
    }

    @Override
    public Thread newThread(Runnable r) {
        String name2 = name + id.getAndIncrement();
        final Runnable r2 = new DefaultRunnableDecorator(r);
        Runnable r3 = new Runnable() {

            @Override
            public void run() {
                AffinityLock al;
                synchronized (AffinityNettyThreadFactory.this) {
                    al = lastAffinityLock == null ? AffinityLock.acquireLock() : lastAffinityLock.acquireLock(strategies);
                    if (al.cpuId() >= 0) {
                        if (!al.isBound()) {
                            al.bind();
                        }
                        lastAffinityLock = al;
                    }
                }
                try {
                    r2.run();
                } finally {
                    al.release();
                }
            }
        };

        Thread t = new FastThreadLocalThread(group, r3, name2);

        try {
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) { /* doesn't matter even if failed to set. */ }

        logger.debug("Creates new {}.", t);

        return t;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }

    private static final class DefaultRunnableDecorator implements Runnable {

        private final Runnable r;

        DefaultRunnableDecorator(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } finally {
                FastThreadLocal.removeAll();
            }
        }
    }
}
