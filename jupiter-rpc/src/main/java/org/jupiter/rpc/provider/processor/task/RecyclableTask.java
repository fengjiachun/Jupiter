/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.rpc.provider.processor.task;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.jupiter.common.concurrent.RejectedRunnable;
import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.RecycleUtil;
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.Recyclers;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.Response;
import org.jupiter.rpc.Status;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.error.BadRequestException;
import org.jupiter.rpc.error.ServerBusyException;
import org.jupiter.rpc.error.ServiceNotFoundException;
import org.jupiter.rpc.error.TPSLimitException;
import org.jupiter.rpc.metric.Metrics;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.limiter.TPSLimiter;
import org.jupiter.rpc.provider.limiter.TPSResult;
import org.jupiter.rpc.provider.processor.ProviderProcessor;
import org.jupiter.serialization.SerializerHolder;

import java.util.concurrent.Executor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.rpc.Status.*;

/**
 * Recyclable Task, reduce distribution and recovery of small objects (help gc).
 *
 * jupiter
 * org.jupiter.rpc.provider.processor.task
 *
 * @author jiachun.fjc
 */
public class RecyclableTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RecyclableTask.class);

    // SPI
    private static final TPSLimiter tpsLimiter = JServiceLoader.load(TPSLimiter.class);

    // - Metrics -------------------------------------------------------------------------------------------------------
    // 请求处理的时间统计(从request被decode开始一直到response数据被刷到OS内核缓冲区为止)
    private static final Timer processingTimer              = Metrics.timer("processing");
    // 请求被拒绝的统计
    private static final Meter rejectionMeter               = Metrics.meter("rejection");
    // 请求数据大小的统计(不包括Jupiter协议头)
    private static final Histogram requestSizeHistogram     = Metrics.histogram("request.size");
    // 响应数据大小的统计(不包括Jupiter协议头)
    private static final Histogram responseSizeHistogram    = Metrics.histogram("response.size");

    private ProviderProcessor processor;
    private JChannel jChannel;
    private Request request;

    @Override
    public void run() {
        // - Deserialization -------------------------------------------------------------------------------------------
        try {
            byte[] bytes = request.bytes();

            // request sizes histogram
            requestSizeHistogram.update(bytes.length);

            MessageWrapper msg = SerializerHolder.serializer().readObject(bytes, MessageWrapper.class);
            request.message(msg);
            request.bytes(null);
        } catch (Exception e) {
            rejected(BAD_REQUEST);
            return;
        }

        // - TPS limit -------------------------------------------------------------------------------------------------
        TPSResult tpsResult = tpsLimiter.process(request);
        if (!tpsResult.isAllowed()) {
            rejected(SERVICE_TPS_LIMIT, tpsResult);
            return;
        }

        // - Lookup the service ----------------------------------------------------------------------------------------
        final MessageWrapper msg = request.message();
        final ServiceWrapper serviceWrapper = processor.lookupService(msg);
        if (serviceWrapper == null) {
            rejected(SERVICE_NOT_FOUND);
            return;
        }

        // - Processing ------------------------------------------------------------------------------------------------
        Executor childExecutor = serviceWrapper.getExecutor();
        if (childExecutor == null) {
            process(msg, serviceWrapper.getServiceProvider());
        } else {
            childExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    process(msg, serviceWrapper.getServiceProvider());
                }
            });
        }
    }

    @Override
    public void rejected() {
        rejected(SERVER_BUSY, null);
    }

    private void rejected(Status status) {
        rejected(status, null);
    }

    private void rejected(Status status, Object signal) {
        rejectionMeter.mark();
        try {
            ResultWrapper result = ResultWrapper.getInstance();
            switch (status) {
                case SERVER_BUSY:
                    result.setError(new ServerBusyException());
                    break;
                case BAD_REQUEST:
                    result.setError(new BadRequestException());
                    break;
                case SERVICE_NOT_FOUND:
                    result.setError(new ServiceNotFoundException(request.message().toString()));
                    break;
                case SERVICE_TPS_LIMIT:
                    if (signal != null && signal instanceof TPSResult) {
                        result.setError(new TPSLimitException(((TPSResult) signal).getMessage()));
                    } else {
                        result.setError(new TPSLimitException());
                    }
                    break;
                default:
                    logger.warn("Unexpected status.", status.description());
                    return;
            }

            logger.warn("Service rejected: {}.", stackTrace(result.getError()));

            final long invokeId = request.invokeId();
            Response response = new Response(invokeId);
            response.status(status.value());
            try {
                // 在业务线程里序列化, 减轻IO线程负担
                response.bytes(SerializerHolder.serializer().writeObject(result));
            } finally {
                RecycleUtil.recycle(result);
            }

            jChannel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationComplete(JChannel ch, boolean isSuccess) throws Exception {
                    if (isSuccess) {
                        logger.debug("Service rejection sent out: {}.", invokeId);
                    } else {
                        logger.warn("Service rejection sent failed: {}.", invokeId);
                    }
                }
            });
        } finally {
            recycle();
        }
    }

    private void process(MessageWrapper msg, Object provider) {
        try {
            Object invokeResult = null;
            String methodName = msg.getMethodName();
            Timer.Context timeCtx = Metrics.timer(msg.directory() + '.' + methodName).time();
            try {
                invokeResult = Reflects.fastInvoke(provider, methodName, msg.getParameterTypes(), msg.getArgs());
            } finally {
                timeCtx.stop();
            }

            ResultWrapper result = ResultWrapper.getInstance();
            result.setResult(invokeResult);

            final long invokeId = request.invokeId();
            Response response = new Response(invokeId);
            response.status(OK.value());
            byte[] bytes;
            try {
                // 在业务线程里序列化, 减轻IO线程负担
                bytes = SerializerHolder.serializer().writeObject(result);
                response.bytes(bytes);
            } finally {
                RecycleUtil.recycle(result);
            }

            final long timestamp = request.timestamp();
            final int bodyLength = bytes.length;
            jChannel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationComplete(JChannel ch, boolean isSuccess) throws Exception {
                    long duration = SystemClock.millisClock().now() - timestamp;
                    if (isSuccess) {
                        responseSizeHistogram.update(bodyLength);
                        processingTimer.update(duration, MILLISECONDS);

                        logger.debug("Service response[id: {}, length: {}] sent out, duration: {} millis.",
                                invokeId, bodyLength, duration);
                    } else {
                        logger.warn("Service response[id: {}, length: {}] sent failed, duration: {} millis.",
                                invokeId, bodyLength, duration);
                    }
                }
            });
        } catch (Exception e) {
            processor.handleException(jChannel, request, e);
        } finally {
            recycle();
        }
    }

    public static RecyclableTask getInstance(ProviderProcessor processor, JChannel jChannel, Request request) {
        RecyclableTask task = recyclers.get();

        task.processor = processor;
        task.jChannel = jChannel;
        task.request = request;
        return task;
    }

    private RecyclableTask(Recyclers.Handle<RecyclableTask> handle) {
        this.handle = handle;
    }

    private boolean recycle() {
        // help GC
        this.processor = null;
        this.jChannel = null;
        this.request = null;

        return recyclers.recycle(this, handle);
    }

    private static final Recyclers<RecyclableTask> recyclers = new Recyclers<RecyclableTask>() {

        @Override
        protected RecyclableTask newObject(Handle<RecyclableTask> handle) {
            return new RecyclableTask(handle);
        }
    };

    private transient final Recyclers.Handle<RecyclableTask> handle;
}
