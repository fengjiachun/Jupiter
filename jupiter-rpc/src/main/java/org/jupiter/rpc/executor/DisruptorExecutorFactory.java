package org.jupiter.rpc.executor;

import org.jupiter.common.concurrent.disruptor.TaskDispatcher;
import org.jupiter.common.concurrent.disruptor.WaitStrategyType;

import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.PROCESSOR_MAX_NUM_WORKS;
import static org.jupiter.common.util.JConstants.PROCESSOR_WORKER_QUEUE_CAPACITY;

/**
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
public class DisruptorExecutorFactory implements ExecutorFactory {

    @Override
    public Executor newExecutor(int parallelism, Object... args) {
        WaitStrategyType waitStrategyType = null;
        if (args.length > 0 && args[0] instanceof WaitStrategyType) {
            waitStrategyType = (WaitStrategyType) args[0];
        }
        if (waitStrategyType == null) {
            waitStrategyType = WaitStrategyType.LITE_BLOCKING_WAIT;
        }

        return new TaskDispatcher(
                parallelism,
                "processor",
                PROCESSOR_WORKER_QUEUE_CAPACITY,
                PROCESSOR_MAX_NUM_WORKS,
                waitStrategyType);
    }
}
