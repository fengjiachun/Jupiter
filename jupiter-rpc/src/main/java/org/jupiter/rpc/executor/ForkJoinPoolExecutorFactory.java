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
package org.jupiter.rpc.executor;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

import org.jupiter.common.util.SpiMetadata;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.InternalForkJoinWorkerThread;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

/**
 * Provide a {@link ForkJoinPool} implementation of executor.
 *
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
@SpiMetadata(name = "forkJoin")
public class ForkJoinPoolExecutorFactory extends AbstractExecutorFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ForkJoinPoolExecutorFactory.class);

    @Override
    public CloseableExecutor newExecutor(Target target, String name) {
        final ForkJoinPool executor = new ForkJoinPool(
                coreWorkers(target),
                new DefaultForkJoinWorkerThreadFactory(name),
                new DefaultUncaughtExceptionHandler(), true);

        return new CloseableExecutor() {

            @Override
            public void execute(Runnable r) {
                executor.execute(r);
            }

            @Override
            public void shutdown() {
                logger.warn("ForkJoinPoolExecutorFactory#{} shutdown.", executor);
                executor.shutdownNow();
            }
        };
    }

    private static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

        private final AtomicInteger idx = new AtomicInteger();
        private final String namePrefix;

        public DefaultForkJoinWorkerThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            // Note: The ForkJoinPool will create these threads as daemon threads.
            ForkJoinWorkerThread thread = new InternalForkJoinWorkerThread(pool);
            thread.setName(namePrefix + '-' + idx.getAndIncrement());
            return thread;
        }
    }

    private static final class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.error("Uncaught exception in thread[{}], {}.", t.getName(), StackTraceUtil.stackTrace(e));
        }
    }
}
