package org.jupiter.common.concurrent;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 任务饱和时以FIFO的方式抛弃队列中一部分现有任务.
 *
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class DiscardTaskPolicyWithReport implements RejectedExecutionHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DiscardTaskPolicyWithReport.class);

    private final String threadPoolName;

    public DiscardTaskPolicyWithReport(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

        logger.error("Thread pool [{}] is exhausted! {}.", threadPoolName, e.toString());

        if (!e.isShutdown()) {
            BlockingQueue<Runnable> queue = e.getQueue();
            int discardSize = queue.size() >> 1;
            for (int i = 0; i < discardSize; i++) {
                queue.poll();
            }
            queue.offer(r);
        }
    }
}
