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
import org.jupiter.common.util.StringBuilderHelper;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.exception.BadRequestException;
import org.jupiter.rpc.exception.FlowControlException;
import org.jupiter.rpc.exception.ServerBusyException;
import org.jupiter.rpc.exception.ServiceNotFoundException;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.metric.Metrics;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.processor.ProviderProcessor;

import java.util.List;
import java.util.concurrent.Executor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.Reflects.fastInvoke;
import static org.jupiter.common.util.Reflects.findMatchingParameterTypes;
import static org.jupiter.rpc.Status.*;
import static org.jupiter.serialization.SerializerHolder.serializerImpl;

/**
 *
 * jupiter
 * org.jupiter.rpc.provider.processor.task
 *
 * @author jiachun.fjc
 */
public class MessageTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

    // - Metrics -------------------------------------------------------------------------------------------------------
    // 请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)
    private static final Timer processingTimer              = Metrics.timer("processing");
    // 请求被拒绝次数统计
    private static final Meter rejectionMeter               = Metrics.meter("rejection");
    // 请求数据大小统计(不包括Jupiter协议头的16个字节)
    private static final Histogram requestSizeHistogram     = Metrics.histogram("request.size");
    // 响应数据大小统计(不包括Jupiter协议头的16个字节)
    private static final Histogram responseSizeHistogram    = Metrics.histogram("response.size");

    private final ProviderProcessor processor;
    private final JChannel channel;
    private final JRequest request;

    public MessageTask(ProviderProcessor processor, JChannel channel, JRequest request) {
        this.processor = processor;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        // stack copy
        final ProviderProcessor _processor = processor;
        final JRequest _request = request;

        // deserialization
        final MessageWrapper msg;
        try {
            byte[] bytes = _request.bytes();
            _request.bytes(null);
            requestSizeHistogram.update(bytes.length);
            msg = serializerImpl().readObject(bytes, MessageWrapper.class);
            _request.message(msg);
        } catch (Throwable t) {
            rejected(BAD_REQUEST);
            return;
        }

        // lookup service
        final ServiceWrapper service = _processor.lookupService(msg.getMetadata());
        if (service == null) {
            rejected(SERVICE_NOT_FOUND);
            return;
        }

        // app flow control
        ControlResult ctrlResult = _processor.flowControl(_request);
        if (!ctrlResult.isAllowed()) {
            rejected(APP_FLOW_CONTROL, ctrlResult);
            return;
        }

        // child(provider) flow control
        FlowController<JRequest> childController = service.getFlowController();
        if (childController != null) {
            ctrlResult = childController.flowControl(_request);
            if (!ctrlResult.isAllowed()) {
                rejected(PROVIDER_FLOW_CONTROL, ctrlResult);
                return;
            }
        }

        // processing
        Executor childExecutor = service.getExecutor();
        if (childExecutor == null) {
            process(service);
        } else {
            childExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    process(service);
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
        final JRequest _request = request; // stack copy

        rejectionMeter.mark();
        ResultWrapper result = new ResultWrapper();
        switch (status) {
            case SERVER_BUSY:
                result.setError(new ServerBusyException());
                break;
            case BAD_REQUEST:
                result.setError(new BadRequestException());
                break;
            case SERVICE_NOT_FOUND:
                result.setError(new ServiceNotFoundException(_request.message().toString()));
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

        logger.warn("Service rejected: {}.", result.getError());

        byte[] bytes = serializerImpl().writeObject(result);

        final long invokeId = _request.invokeId();
        channel.write(JResponse.getInstance(invokeId, status, bytes), new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                logger.debug("Service rejection sent out: {}, {}.", invokeId, channel);
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                logger.warn("Service rejection sent failed: {}, {}, {}.", invokeId, channel, cause);
            }
        });
    }

    private void process(ServiceWrapper service) {
        final JRequest _request = request; // stack copy

        try {
            MessageWrapper msg = _request.message();
            String methodName = msg.getMethodName();
            TraceId traceId = msg.getTraceId();
            String directory = msg.getMetadata().directory(); // 避免StringBuilderHelper被嵌套使用
            String callInfo = StringBuilderHelper.get()
                    .append(directory)
                    .append('#')
                    .append(methodName).toString();

            final long invokeId = _request.invokeId();

            // tracing
            if (traceId != null) {
                TracingEye.setCurrent(traceId.incrementNodeAndGet());

                if (logger.isInfoEnabled()) {
                    String traceText = traceId.asText(); // 避免StringBuilderHelper被嵌套使用

                    String traceInfo = StringBuilderHelper.get()
                            .append("[Provider] - ")
                            .append(traceText)
                            .append(", invokeId: ")
                            .append(invokeId)
                            .append(", callInfo: ")
                            .append(callInfo)
                            .append(", on ")
                            .append(channel).toString();

                    logger.info(traceInfo);
                }
            }

            Object invokeResult = null;
            Timer.Context timeCtx = Metrics.timer(callInfo).time();
            try {
                Object[] args = msg.getArgs();
                List<Class<?>[]> parameterTypesList = service.getMethodParameterTypes(methodName);
                if (parameterTypesList == null) {
                    throw new NoSuchMethodException(methodName);
                }
                invokeResult = fastInvoke(
                        service.getServiceProvider(),
                        methodName,
                        findMatchingParameterTypes(parameterTypesList, args),
                        args);
            } finally {
                timeCtx.stop();
            }

            ResultWrapper result = new ResultWrapper();
            result.setResult(invokeResult);
            final byte[] bytes = serializerImpl().writeObject(result);

            channel.write(JResponse.getInstance(invokeId, OK, bytes), new JFutureListener<JChannel>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    long duration = SystemClock.millisClock().now() - _request.timestamp();

                    responseSizeHistogram.update(bytes.length);
                    processingTimer.update(duration, MILLISECONDS);

                    logger.debug("Service response[id: {}, length: {}] sent out, duration: {} millis.",
                            invokeId, bytes.length, duration);
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                    long duration = SystemClock.millisClock().now() - _request.timestamp();

                    logger.warn("Service response[id: {}, length: {}] sent failed, duration: {} millis, {}, {}.",
                            invokeId, bytes.length, duration, channel, cause);
                }
            });
        } catch (Throwable t) {
            processor.handleException(channel, _request, t);
        }
    }
}
