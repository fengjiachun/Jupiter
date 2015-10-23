package org.jupiter.common.concurrent;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 使用阻塞生产者的饱和策略, 不抛弃任务, 也不抛出异常, 当队列满时改为调用BlockingQueue.put来实现生产者的阻塞.
 *
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class BlockingProducersPolicyWithReport implements RejectedExecutionHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BlockingProducersPolicyWithReport.class);

    private final String threadPoolName;

    public BlockingProducersPolicyWithReport(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

        logger.error("Thread pool [{}] is exhausted! {}.", threadPoolName, e.toString());

        if (!e.isShutdown()) {
            try {
                e.getQueue().put(r);
            } catch (InterruptedException ignored) { /* should not be interrupted */ }
        }
    }
}
