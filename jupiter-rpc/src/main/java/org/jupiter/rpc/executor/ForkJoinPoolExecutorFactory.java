package org.jupiter.rpc.executor;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
public class ForkJoinPoolExecutorFactory implements ExecutorFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ForkJoinPoolExecutorFactory.class);

    @Override
    public Executor newExecutor(int parallelism, Object... args) {
        return new ForkJoinPool(
                parallelism,
                new DefaultForkJoinWorkerThreadFactory("fjp.processor"),
                new DefaultUncaughtExceptionHandler(), true);
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
            ForkJoinWorkerThread thread = new DefaultForkJoinWorkerThread(pool);
            thread.setName(namePrefix + '-' + idx.getAndIncrement());
            return thread;
        }
    }

    private static final class DefaultForkJoinWorkerThread extends ForkJoinWorkerThread {

        public DefaultForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }
    }

    private static final class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.error("Uncaught exception in thread: {}", t.getName(), e);
        }
    }
}
