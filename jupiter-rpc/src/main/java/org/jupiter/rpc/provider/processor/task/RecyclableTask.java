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
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.Recyclers;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.Status;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.error.BadRequestException;
import org.jupiter.rpc.error.ServerBusyException;
import org.jupiter.rpc.error.ServiceNotFoundException;
import org.jupiter.rpc.error.FlowControlException;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.metric.Metrics;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.processor.ProviderProcessor;

import java.util.concurrent.Executor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.Reflects.fastInvoke;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.rpc.Status.*;
import static org.jupiter.serialization.SerializerHolder.serializer;

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

    // - Metrics -------------------------------------------------------------------------------------------------------
    // 请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)
    private static final Timer processingTimer              = Metrics.timer("processing");
    // 请求被拒绝次数统计
    private static final Meter rejectionMeter               = Metrics.meter("rejection");
    // 请求数据大小统计(不包括Jupiter协议头的16个字节)
    private static final Histogram requestSizeHistogram     = Metrics.histogram("request.size");
    // 响应数据大小统计(不包括Jupiter协议头的16个字节)
    private static final Histogram responseSizeHistogram    = Metrics.histogram("response.size");

    private ProviderProcessor processor;
    private JChannel channel;
    private JRequest request;

    @Override
    public void run() {
        // deserialization
        final MessageWrapper msg;
        try {
            byte[] bytes = request.bytes();
            request.bytes(null);
            requestSizeHistogram.update(bytes.length);
            msg = serializer().readObject(bytes, MessageWrapper.class);
            request.message(msg);
        } catch (Throwable t) {
            rejected(BAD_REQUEST);
            return;
        }

        // lookup service
        final ServiceWrapper service = processor.lookupService(msg.getMetadata());
        if (service == null) {
            rejected(SERVICE_NOT_FOUND);
            return;
        }

        // app flow control
        ControlResult ctrlResult = processor.flowControl(request);
        if (!ctrlResult.isAllowed()) {
            rejected(APP_FLOW_CONTROL, ctrlResult);
            return;
        }

        // child(provider) flow control
        FlowController<JRequest> childController = service.getFlowController();
        if (childController != null) {
            ctrlResult = childController.flowControl(request);
            if (!ctrlResult.isAllowed()) {
                rejected(PROVIDER_FLOW_CONTROL, ctrlResult);
                return;
            }
        }

        // processing
        Executor childExecutor = service.getExecutor();
        if (childExecutor == null) {
            process(service.getServiceProvider());
        } else {
            childExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    process(service.getServiceProvider());
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
                case APP_FLOW_CONTROL:
                case PROVIDER_FLOW_CONTROL:
                    if (signal != null && signal instanceof ControlResult) {
                        result.setError(new FlowControlException(((ControlResult) signal).getMessage()));
                    } else {
                        result.setError(new FlowControlException());
                    }
                    break;
                default:
                    logger.warn("Unexpected status.", status.description());
                    return;
            }

            logger.warn("Service rejected: {}.", stackTrace(result.getError()));

            final long invokeId = request.invokeId();
            JResponse response = new JResponse(invokeId);
            response.status(status.value());
            try {
                // 在业务线程里序列化, 减轻IO线程负担
                byte[] bytes = serializer().writeObject(result);
                response.bytes(bytes);
            } finally {
                RecycleUtil.recycle(result);
            }

            channel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationComplete(JChannel channel, boolean isSuccess) throws Exception {
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

    private void process(Object provider) {
        try {
            MessageWrapper msg = request.message();
            String methodName = msg.getMethodName();

            Object invokeResult = null;
            Timer.Context timeCtx = Metrics.timer(msg.getMetadata().directory() + '#' + methodName).time();
            try {
                invokeResult = fastInvoke(provider, methodName, msg.getParameterTypes(), msg.getArgs());
            } finally {
                timeCtx.stop();
            }

            final long invokeId = request.invokeId();
            JResponse response = new JResponse(invokeId);
            response.status(OK.value());

            int bytesLength = 0;
            ResultWrapper result = ResultWrapper.getInstance();
            try {
                result.setResult(invokeResult);
                // 在业务线程里序列化, 减轻IO线程负担
                byte[] bytes = serializer().writeObject(result);
                bytesLength = bytes.length;

                response.bytes(bytes);
            } finally {
                RecycleUtil.recycle(result);
            }

            final long timestamp = request.timestamp();
            final int bodyLength = bytesLength;
            channel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationComplete(JChannel channel, boolean isSuccess) throws Exception {
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
        } catch (Throwable t) {
            processor.handleException(channel, request, t);
        } finally {
            recycle();
        }
    }

    public static RecyclableTask getInstance(ProviderProcessor processor, JChannel channel, JRequest request) {
        RecyclableTask task = recyclers.get();

        task.processor = processor;
        task.channel = channel;
        task.request = request;
        return task;
    }

    private RecyclableTask(Recyclers.Handle<RecyclableTask> handle) {
        this.handle = handle;
    }

    private boolean recycle() {
        // help GC
        this.processor = null;
        this.channel = null;
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
