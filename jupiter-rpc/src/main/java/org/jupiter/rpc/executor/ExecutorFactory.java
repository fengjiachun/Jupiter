package org.jupiter.rpc.executor;

import org.jupiter.rpc.consumer.processor.ConsumerExecutorFactory;
import org.jupiter.rpc.provider.processor.ProviderExecutorFactory;

import java.util.concurrent.Executor;

/**
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
public interface ExecutorFactory extends ConsumerExecutorFactory, ProviderExecutorFactory {

    Executor newExecutor(int parallelism, Object... args);
}
