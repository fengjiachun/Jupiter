package org.jupiter.rpc.consumer.processor;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.rpc.Response;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.processor.task.RecyclableTask;
import org.jupiter.rpc.executor.ExecutorFactory;

import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.PROCESSOR_CORE_NUM_WORKERS;

/**
 * Default consumer's processor.
 *
 * jupiter
 * org.jupiter.rpc.consumer.processor
 *
 * @author jiachun.fjc
 */
public class DefaultConsumerProcessor implements ConsumerProcessor {

    private final Executor executor;

    public DefaultConsumerProcessor() {
        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.load(ConsumerExecutorFactory.class);
        executor = factory.newExecutor(PROCESSOR_CORE_NUM_WORKERS);
    }

    @Override
    public void handleResponse(JChannel ch, Response response) throws Exception {
        executor.execute(RecyclableTask.getInstance(ch, response));
    }
}
