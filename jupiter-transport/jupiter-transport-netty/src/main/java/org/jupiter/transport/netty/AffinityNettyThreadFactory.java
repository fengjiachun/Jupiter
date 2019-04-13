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
import net.openhft.affinity.AffinityStrategy;

import org.jupiter.common.concurrent.AffinityNamedThreadFactory;

/**
 * This is a ThreadFactory which assigns threads based the strategies provided.
 *
 * If no strategies are provided AffinityStrategies.ANY is used.
 *
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class AffinityNettyThreadFactory extends AffinityNamedThreadFactory {

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
        super(name, daemon, priority, strategies);
    }

    @Override
    protected Runnable wrapRunnable(Runnable r) {
        return new DefaultRunnableDecorator(r);
    }

    @Override
    protected Thread wrapThread(ThreadGroup group, Runnable r, String name) {
        return new FastThreadLocalThread(group, r, name);
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
