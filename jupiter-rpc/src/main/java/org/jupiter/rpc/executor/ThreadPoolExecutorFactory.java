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
import org.jupiter.common.util.SystemPropertyUtil;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;
import static org.jupiter.common.util.JConstants.PROCESSOR_MAX_NUM_WORKS;
import static org.jupiter.common.util.JConstants.PROCESSOR_WORKER_QUEUE_CAPACITY;

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

    @Override
    public Executor newExecutor(int parallelism) {
        BlockingQueue<Runnable> workQueue = null;

        String workerQueueTypeString = SystemPropertyUtil.get("jupiter.executor.thread.pool.queue.type");
        WorkerQueueType workerQueueType = WorkerQueueType.ARRAY_BLOCKING_QUEUE;
        for (WorkerQueueType qType : WorkerQueueType.values()) {
            if (qType.name().equals(workerQueueTypeString)) {
                workerQueueType = qType;
                break;
            }
        }
        switch (workerQueueType) {
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
                new RejectedTaskPolicyWithReport("processor"));
    }

    public enum WorkerQueueType {
        LINKED_BLOCKING_QUEUE,
        ARRAY_BLOCKING_QUEUE
    }
}
