package org.jupiter.rpc.provider.processor;

import com.codahale.metrics.Histogram;
import org.jupiter.common.util.JServiceLoader;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.executor.ExecutorFactory;
import org.jupiter.rpc.metric.Metrics;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.limiter.TPSLimiter;
import org.jupiter.rpc.provider.limiter.TPSResult;
import org.jupiter.rpc.provider.processor.task.RecyclableTask;
import org.jupiter.serialization.SerializerHolder;

import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.PROCESSOR_CORE_NUM_WORKERS;
import static org.jupiter.rpc.Status.BAD_REQUEST;
import static org.jupiter.rpc.Status.SERVICE_NOT_FOUND;
import static org.jupiter.rpc.Status.SERVICE_TPS_LIMIT;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public class DefaultProviderProcessor extends AbstractProviderProcessor {

    // 请求数据大小的统计(不包括Jupiter协议头)
    private static final Histogram requestSizes = Metrics.histogram(DefaultProviderProcessor.class, "request.size");

    // SPI
    private final TPSLimiter tpsLimiter = JServiceLoader.load(TPSLimiter.class);
    private volatile Executor executor;

    public DefaultProviderProcessor(JServer jServer) {
        super(jServer);

        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.load(ProviderExecutorFactory.class);
        executor = factory.newExecutor(PROCESSOR_CORE_NUM_WORKERS);
    }

    public DefaultProviderProcessor(JServer jServer, Executor executor) {
        super(jServer);
        this.executor = executor;
    }

    @Override
    public void handleRequest(JChannel ch, Request request) throws Exception {
        RecyclableTask task;
        try {
            byte[] bytes = request.bytes();

            // request sizes histogram
            requestSizes.update(bytes.length);

            // 反序列化
            MessageWrapper msg = SerializerHolder.serializer().readObject(bytes, MessageWrapper.class);
            request.message(msg);
            request.bytes(null);
        } catch (Exception e) {
            request.status(BAD_REQUEST);
            task = RecyclableTask.getInstance(this, ch, request, null);
            task.reject();
            return;
        }

        // TPS限流处理
        TPSResult tpsResult = tpsLimiter.process(request);
        if (!tpsResult.isAllowed()) {
            request.status(SERVICE_TPS_LIMIT);
            task = RecyclableTask.getInstance(this, ch, request, null, tpsResult);
            task.reject();
            return;
        }

        MessageWrapper msg = request.message();
        // 查找服务
        ServiceWrapper serviceWrapper = lookupService(msg);

        if (serviceWrapper == null) {
            request.status(SERVICE_NOT_FOUND);
            task = RecyclableTask.getInstance(this, ch, request, null);
            task.reject();
        } else {
            task = RecyclableTask.getInstance(this, ch, request, serviceWrapper.getServiceProvider());
            Executor providerExecutor = serviceWrapper.getExecutor();
            if (providerExecutor != null) {
                providerExecutor.execute(task);
            } else if (executor != null) {
                executor.execute(task);
            } else {
                task.run();
            }
        }
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
