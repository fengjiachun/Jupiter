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

import java.lang.reflect.Constructor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jupiter.common.concurrent.RejectedTaskPolicyWithReport;
import org.jupiter.common.util.SpiMetadata;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

/**
 * Provide a {@link ThreadPoolExecutor} implementation of executor.
 * Thread pool executor factory.
 *
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
@SpiMetadata(name = "threadPool", priority = 1)
public class ThreadPoolExecutorFactory extends AbstractExecutorFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadPoolExecutorFactory.class);

    @Override
    public CloseableExecutor newExecutor(Target target, String name) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreWorkers(target),
                maxWorkers(target),
                120L,
                TimeUnit.SECONDS,
                workQueue(target),
                threadFactory(name),
                createRejectedPolicy(target, name, new RejectedTaskPolicyWithReport(name, "jupiter")));

        return new CloseableExecutor() {

            @Override
            public void execute(Runnable r) {
                executor.execute(r);
            }

            @Override
            public void shutdown() {
                logger.warn("ThreadPoolExecutorFactory#{} shutdown.", executor);
                executor.shutdownNow();
            }
        };
    }

    private BlockingQueue<Runnable> workQueue(Target target) {
        BlockingQueue<Runnable> workQueue = null;
        WorkQueueType queueType = queueType(target, WorkQueueType.ARRAY_BLOCKING_QUEUE);
        int queueCapacity = queueCapacity(target);
        switch (queueType) {
            case LINKED_BLOCKING_QUEUE:
                workQueue = new LinkedBlockingQueue<>(queueCapacity);
                break;
            case ARRAY_BLOCKING_QUEUE:
                workQueue = new ArrayBlockingQueue<>(queueCapacity);
                break;
        }

        return workQueue;
    }

    @SuppressWarnings("SameParameterValue")
    private WorkQueueType queueType(Target target, WorkQueueType defaultType) {
        WorkQueueType queueType = null;
        switch (target) {
            case CONSUMER:
                queueType = WorkQueueType.parse(SystemPropertyUtil.get(CONSUMER_EXECUTOR_QUEUE_TYPE));
                break;
            case PROVIDER:
                queueType = WorkQueueType.parse(SystemPropertyUtil.get(PROVIDER_EXECUTOR_QUEUE_TYPE));
                break;
        }

        return queueType == null ? defaultType : queueType;
    }

    private RejectedExecutionHandler createRejectedPolicy(Target target, String name, RejectedExecutionHandler defaultHandler) {
        RejectedExecutionHandler handler = null;
        String handlerClass = null;
        switch (target) {
            case CONSUMER:
                handlerClass = SystemPropertyUtil.get(CONSUMER_THREAD_POOL_REJECTED_HANDLER);
                break;
            case PROVIDER:
                handlerClass = SystemPropertyUtil.get(PROVIDER_THREAD_POOL_REJECTED_HANDLER);
                break;
        }
        if (Strings.isNotBlank(handlerClass)) {
            try {
                Class<?> cls = Class.forName(handlerClass);
                try {
                    Constructor<?> constructor = cls.getConstructor(String.class, String.class);
                    handler = (RejectedExecutionHandler) constructor.newInstance(name, "jupiter");
                } catch (NoSuchMethodException e) {
                    handler = (RejectedExecutionHandler) cls.newInstance();
                }
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Construct {} failed, {}.", handlerClass, StackTraceUtil.stackTrace(e));
                }
            }
        }

        return handler == null ? defaultHandler : handler;
    }

    enum WorkQueueType {
        LINKED_BLOCKING_QUEUE,
        ARRAY_BLOCKING_QUEUE;

       static WorkQueueType parse(String name) {
            for (WorkQueueType type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
