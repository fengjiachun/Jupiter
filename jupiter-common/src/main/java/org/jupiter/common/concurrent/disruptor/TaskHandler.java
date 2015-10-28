package org.jupiter.common.concurrent.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.WorkHandler;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

/**
 * Callback interface to be implemented for processing events as they become available in the RingBuffer.
 *
 * jupiter
 * org.jupiter.common.concurrent.disruptor
 *
 * @author jiachun.fjc
 */
public class TaskHandler implements
        EventHandler<MessageEvent<Runnable>>, WorkHandler<MessageEvent<Runnable>>, LifecycleAware {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TaskHandler.class);

    @Override
    public void onEvent(MessageEvent<Runnable> event, long sequence, boolean endOfBatch) throws Exception {
        event.getMessage().run();
    }

    @Override
    public void onEvent(MessageEvent<Runnable> event) throws Exception {
        event.getMessage().run();
    }

    @Override
    public void onStart() {
        logger.info("Task handler on start: {}.", Thread.currentThread().getName());
    }

    @Override
    public void onShutdown() {
        logger.info("Task handler on shutdown: {}.", Thread.currentThread().getName());
    }
}
