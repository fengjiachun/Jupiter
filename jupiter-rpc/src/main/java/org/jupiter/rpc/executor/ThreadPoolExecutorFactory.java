package org.jupiter.rpc.executor;

import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.common.concurrent.RejectedTaskPolicyWithReport;

import java.util.concurrent.*;

import static org.jupiter.common.util.JConstants.PROCESSOR_MAX_NUM_WORKS;
import static org.jupiter.common.util.JConstants.PROCESSOR_WORKER_QUEUE_CAPACITY;

/**
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
public class ThreadPoolExecutorFactory implements ExecutorFactory {

    @Override
    public Executor newExecutor(int parallelism, Object... args) {
        BlockingQueue<Runnable> workQueue = null;
        if (args.length > 0 && args[0] instanceof WorkQueueType) {
            switch ((WorkQueueType) args[0]) {
                case LINKED_BLOCKING_QUEUE:
                    workQueue = new LinkedBlockingQueue<Runnable>(PROCESSOR_WORKER_QUEUE_CAPACITY);
                    break;
                case ARRAY_BLOCKING_QUEUE:
                    workQueue = new ArrayBlockingQueue<Runnable>(PROCESSOR_WORKER_QUEUE_CAPACITY);
                    break;
            }
        }
        if (workQueue == null) {
            workQueue = new LinkedBlockingQueue<Runnable>(PROCESSOR_WORKER_QUEUE_CAPACITY);
        }

        return new ThreadPoolExecutor(
                parallelism,
                PROCESSOR_MAX_NUM_WORKS,
                120L,
                TimeUnit.SECONDS,
                workQueue,
                new NamedThreadFactory("processor"),
                new RejectedTaskPolicyWithReport("processor"));
    }

    public enum WorkQueueType {
        LINKED_BLOCKING_QUEUE,
        ARRAY_BLOCKING_QUEUE
    }
}
