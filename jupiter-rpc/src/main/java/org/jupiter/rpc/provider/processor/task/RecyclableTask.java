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
import org.jupiter.common.util.RecycleUtil;
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.SystemPropertyUtil;
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
import org.jupiter.rpc.provider.limiter.TPSResult;
import org.jupiter.rpc.provider.processor.ProviderProcessor;
import org.jupiter.serialization.SerializerHolder;

import java.util.concurrent.TimeUnit;

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

    // 请求处理的时间统计(从request被decode开始一直到response数据被刷到OS内核缓冲区为止)
    private static final Timer invocationTimer;
    // 响应数据大小的统计(不包括Jupiter协议头)
    private static final Histogram responseSizeHistogram;
    // 请求被拒绝的统计
    private static final Meter rejectionMeter;
    static {
        invocationTimer = SystemPropertyUtil.getBoolean("jupiter.metrics.invocation.timer", false)
                ? Metrics.timer(RecyclableTask.class, "invocation") : null;
        responseSizeHistogram = SystemPropertyUtil.getBoolean("jupiter.metrics.response.size.histogram", false)
                ? Metrics.histogram(RecyclableTask.class, "response.size") : null;
        rejectionMeter = SystemPropertyUtil.getBoolean("jupiter.metrics.rejection.meter", true)
                ? Metrics.meter(RecyclableTask.class, "rejection") : null;
    }

    private ProviderProcessor processor;
    private JChannel jChannel;
    private Request request;
    private Object provider;
    private Object attribute;

    @Override
    public void run() {
        if (request.status() != OK) {
            reject();
            return;
        }

        try {
            MessageWrapper msg = request.message();

            Timer.Context timeCtx = Metrics.timer(msg.directory() + '.' + msg.getMethodName()).time();
            Object invokeResult = null;
            try {
                // 调用对应服务
                invokeResult = Reflects.fastInvoke(provider, msg.getMethodName(), msg.getParameterTypes(), msg.getArgs());
            } finally {
                timeCtx.stop();
            }

            ResultWrapper result = ResultWrapper.getInstance();
            result.setResult(invokeResult);

            final long id = request.invokeId();
            Response response = new Response(id);
            response.status(OK.value());
            try {
                // 在业务线程里序列化, 减轻IO线程负担
                response.bytes(SerializerHolder.serializer().writeObject(result));
            } finally {
                RecycleUtil.recycle(result);
            }

            final long timestamp = request.timestamp();
            final int bodySize = response.bytes().length;
            jChannel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationComplete(JChannel ch, boolean isSuccess) throws Exception {
                    long duration = SystemClock.millisClock().now() - timestamp;
                    if (isSuccess) {
                        if (responseSizeHistogram != null) {
                            responseSizeHistogram.update(bodySize);
                        }

                        if (invocationTimer != null) {
                            invocationTimer.update(duration, TimeUnit.MILLISECONDS);
                        }

                        logger.debug("Service response has sent out: {}, response body size: {}, duration: {} millis.",
                                id, bodySize, duration);
                    } else {
                        logger.warn("Service response sent failed: {}, response body size: {}, duration: {} millis.",
                                id, bodySize, duration);
                    }
                }
            });
        } catch (Exception e) {
            processor.handleException(jChannel, request, e);
        } finally {
            recycle();
        }
    }

    @Override
    public void reject() {
        if (rejectionMeter != null) {
            rejectionMeter.mark();
        }

        try {
            ResultWrapper result = ResultWrapper.getInstance();
            Status status = request.status();
            if (status == SERVICE_TPS_LIMIT) {
                if (attribute != null && attribute instanceof TPSResult) {
                    result.setError(new TPSLimitException(((TPSResult) attribute).getMessage()));
                } else {
                    result.setError(new TPSLimitException());
                }
            } else if (status == SERVICE_NOT_FOUND) {
                result.setError(new ServiceNotFoundException(request.message().toString()));
            } else if (status == BAD_REQUEST) {
                result.setError(new BadRequestException());
            } else {
                result.setError(new ServerBusyException());
            }

            logger.warn("Service rejected: {}.", stackTrace(result.getError()));

            final long id = request.invokeId();
            Response response = new Response(id);
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
                        logger.debug("Service rejection has sent out: {}.", id);
                    } else {
                        logger.warn("Service rejection sent failed: {}.", id);
                    }
                }
            });
        } finally {
            recycle();
        }
    }

    public static RecyclableTask getInstance(
            ProviderProcessor processor, JChannel jChannel, Request request, Object provider) {

        RecyclableTask task = recyclers.get();

        task.processor = processor;
        task.jChannel = jChannel;
        task.request = request;
        task.provider = provider;
        return task;
    }

    public static RecyclableTask getInstance(
            ProviderProcessor processor, JChannel jChannel, Request request, Object provider, Object attribute) {

        RecyclableTask task = recyclers.get();

        task.processor = processor;
        task.jChannel = jChannel;
        task.request = request;
        task.provider = provider;
        task.attribute = attribute;
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
        this.provider = null;
        this.attribute = null;

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
