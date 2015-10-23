package org.jupiter.common.concurrent;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 使用Caller-Runs(调用者执行)饱和策略, 不抛弃任务, 也不抛出异常, 而是将当前任务回退到调用者去执行, 从而降低新任务的流量.
 *
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class CallerRunsPolicyWithReport extends ThreadPoolExecutor.CallerRunsPolicy {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CallerRunsPolicyWithReport.class);

    private final String threadPoolName;

    public CallerRunsPolicyWithReport(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

        logger.error("Thread pool [{}] is exhausted! {}.", threadPoolName, e.toString());

        super.rejectedExecution(r, e);
    }
}
