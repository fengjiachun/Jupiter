package org.jupiter.common.concurrent;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 如果该任务实现了 {@link RejectedRunnable} 接口, 那么交给用户去实现拒绝服务的逻辑,
 * 否则以FIFO的方式抛弃队列中一部分现有任务.
 *
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class RejectedTaskPolicyWithReport implements RejectedExecutionHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RejectedTaskPolicyWithReport.class);

    private final String threadPoolName;

    public RejectedTaskPolicyWithReport(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

        logger.error("Thread pool [{}] is exhausted! {}.", threadPoolName, e.toString());

        if (r instanceof RejectedRunnable) {
            ((RejectedRunnable) r).reject(); // 交给用户来处理
        } else {
            if (!e.isShutdown()) {
                BlockingQueue<Runnable> queue = e.getQueue();
                int discardSize = queue.size() >> 1;
                for (int i = 0; i < discardSize; i++) {
                    queue.poll();
                }

                try {
                    queue.put(r);
                } catch (InterruptedException ignored) { /* should not be interrupted */ }
            }
        }
    }
}
