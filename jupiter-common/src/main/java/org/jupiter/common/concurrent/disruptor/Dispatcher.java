package org.jupiter.common.concurrent.disruptor;

import static org.jupiter.common.util.JConstants.AVAILABLE_PROCESSORS;

/**
 * jupiter
 * org.jupiter.common.concurrent.disruptor
 *
 * @author jiachun.fjc
 */
public interface Dispatcher<T> {

    int BUFFER_SIZE = 2048;
    int MAX_NUM_WORKERS = AVAILABLE_PROCESSORS << 3;

    /**
     * 任务分发
     */
    boolean dispatch(T message);

    void shutdown();
}
