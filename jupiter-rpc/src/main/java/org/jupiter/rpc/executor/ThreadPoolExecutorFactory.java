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

import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.common.concurrent.RejectedTaskPolicyWithReport;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.lang.reflect.Constructor;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jupiter.common.util.JConstants.PROCESSOR_MAX_NUM_WORKS;
import static org.jupiter.common.util.JConstants.PROCESSOR_WORKER_QUEUE_CAPACITY;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * Provide a {@link ThreadPoolExecutor} implementation of executor.
 * Thread pool executor factory.
 *
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
public class ThreadPoolExecutorFactory implements ExecutorFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadPoolExecutorFactory.class);

    @Override
    public Executor newExecutor(int parallelism) {
        BlockingQueue<Runnable> workQueue = null;

        String queueTypeString = SystemPropertyUtil.get("jupiter.executor.thread.pool.queue.type");
        WorkerQueueType queueType = WorkerQueueType.parse(queueTypeString);
        if (queueType == null) {
            queueType = WorkerQueueType.ARRAY_BLOCKING_QUEUE;
        }
        switch (queueType) {
            case LINKED_BLOCKING_QUEUE:
                workQueue = new LinkedBlockingQueue<>(PROCESSOR_WORKER_QUEUE_CAPACITY);
                break;
            case ARRAY_BLOCKING_QUEUE:
                workQueue = new ArrayBlockingQueue<>(PROCESSOR_WORKER_QUEUE_CAPACITY);
                break;
        }

        return new ThreadPoolExecutor(
                parallelism,
                PROCESSOR_MAX_NUM_WORKS,
                120L,
                SECONDS,
                workQueue,
                new NamedThreadFactory("processor"),
                createRejectedPolicy());
    }

    private RejectedExecutionHandler createRejectedPolicy() {
        RejectedExecutionHandler handler = null;

        String handlerClass = SystemPropertyUtil.get("jupiter.executor.thread.pool.rejected.handler");
        if (Strings.isNotBlank(handlerClass)) {
            try {
                Class<?> cls = Class.forName(handlerClass);
                try {
                    Constructor<?> constructor = cls.getConstructor(String.class);
                    handler = (RejectedExecutionHandler) constructor.newInstance("processor");
                } catch (NoSuchMethodException e) {
                    handler = (RejectedExecutionHandler) cls.newInstance();
                }
            } catch (Exception e) {
                logger.warn("Construct {} failed, {}.", handlerClass, stackTrace(e));
            }
        }
        if (handler == null) {
            handler = new RejectedTaskPolicyWithReport("processor");
        }

        return handler;
    }

    enum WorkerQueueType {
        LINKED_BLOCKING_QUEUE,
        ARRAY_BLOCKING_QUEUE;

       static WorkerQueueType parse(String name) {
            for (WorkerQueueType type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
