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
import org.jupiter.common.util.internal.UnsafeIntegerFieldUpdater;
import org.jupiter.common.util.internal.UnsafeUpdater;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.exception.JupiterBadRequestException;
import org.jupiter.rpc.exception.JupiterFlowControlException;
import org.jupiter.rpc.exception.JupiterServerBusyException;
import org.jupiter.rpc.exception.JupiterServiceNotFoundException;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.metric.Metrics;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.processor.AbstractProviderProcessor;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingRecorder;
import org.jupiter.rpc.tracing.TracingUtil;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.transport.Status;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JFutureListener;
import org.jupiter.transport.payload.JRequestBytes;
import org.jupiter.transport.payload.JResponseBytes;

import java.util.List;
import java.util.concurrent.Executor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jupiter.common.util.Reflects.fastInvoke;
import static org.jupiter.common.util.Reflects.findMatchingParameterTypes;
import static org.jupiter.rpc.tracing.TracingRecorder.Role.PROVIDER;
import static org.jupiter.transport.Status.*;

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

    // -----------------------------------------------------------------------------------------------------------------

    private static final UnsafeIntegerFieldUpdater<TraceId> traceNodeUpdater =
            UnsafeUpdater.newIntegerFieldUpdater(TraceId.class, "node");

    private final AbstractProviderProcessor processor;
    private final JChannel channel;
    private final JRequest request;

    public MessageTask(AbstractProviderProcessor processor, JChannel channel, JRequest request) {
        this.processor = processor;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        // stack copy
        final AbstractProviderProcessor _processor = processor;
        final JRequest _request = request;

        MessageWrapper msg;
        try {
            JRequestBytes _requestBytes = _request.requestBytes();

            byte s_code = _requestBytes.serializerCode();
            byte[] bytes = _requestBytes.bytes();
            _requestBytes.nullBytes();

            requestSizeHistogram.update(bytes.length);

            Serializer serializer = SerializerFactory.getSerializer(s_code);
            // 在业务线程中反序列化, 减轻IO线程负担
            msg = serializer.readObject(bytes, MessageWrapper.class);
            _request.message(msg);
        } catch (Throwable t) {
            rejected(BAD_REQUEST, new JupiterBadRequestException(t.getMessage()));
            return;
        }

        // 查找服务
        final ServiceWrapper service = _processor.lookupService(msg.getMetadata());
        if (service == null) {
            rejected(SERVICE_NOT_FOUND, new JupiterServiceNotFoundException(String.valueOf(msg)));
            return;
        }

        // 全局流量控制
        ControlResult ctrl = _processor.flowControl(_request);
        if (!ctrl.isAllowed()) {
            rejected(APP_FLOW_CONTROL, new JupiterFlowControlException(String.valueOf(ctrl)));
            return;
        }

        // provider私有流量控制
        FlowController<JRequest> childController = service.getFlowController();
        if (childController != null) {
            ctrl = childController.flowControl(_request);
            if (!ctrl.isAllowed()) {
                rejected(PROVIDER_FLOW_CONTROL, new JupiterFlowControlException(String.valueOf(ctrl)));
                return;
            }
        }

        // processing
        Executor childExecutor = service.getExecutor();
        if (childExecutor == null) {
            process(service);
        } else {
            // provider私有线程池执行
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
        rejected(SERVER_BUSY, new JupiterServerBusyException(String.valueOf(request)));
    }

    // 当服务拒绝方法被调用时一般分以下几种情况:
    //  1. 非法请求;
    //  2. 服务端处理能力出现瓶颈;
    //
    // 回复响应后会自动关闭当前连接, Jupiter客户端会自动重连并重新预热, 在加权负载均衡的情况下权重是一点一点升上来的
    private void rejected(Status status, Throwable cause) {
        // stack copy
        final JRequest _request = request;

        rejectionMeter.mark();

        ResultWrapper result = new ResultWrapper();
        result.setError(cause);

        logger.warn("Service rejected: {}.", result.getError());

        byte s_code = _request.serializerCode();
        Serializer serializer = SerializerFactory.getSerializer(s_code);
        // 在业务线程中序列化, 减轻IO线程负担
        byte[] bytes = serializer.writeObject(result);

        final long invokeId = _request.invokeId();
        JResponseBytes response = new JResponseBytes(invokeId);
        response.status(status.value());
        response.bytes(s_code, bytes);
        channel.write(response, JChannel.CLOSE);
    }

    private void process(ServiceWrapper service) {
        // stack copy
        final JRequest _request = request;

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

            // current traceId
            if (traceId != null && TracingUtil.isTracingNeeded()) {
                traceNodeUpdater.set(traceId, traceId.getNode() + 1);
                TracingUtil.setCurrent(traceId);
            }

            Object invokeResult = null;
            Timer.Context timerCtx = Metrics.timer(callInfo).time();
            try {
                Object provider = service.getServiceProvider();
                Object[] args = msg.getArgs();
                List<Class<?>[]> parameterTypesList = service.getMethodParameterTypes(methodName);
                if (parameterTypesList == null) {
                    throw new NoSuchMethodException(methodName);
                }

                // 根据JLS规则查找最匹配的方法parameterTypes
                Class<?>[] parameterTypes = findMatchingParameterTypes(parameterTypesList, args);

                invokeResult = fastInvoke(provider, methodName, parameterTypes, args);
            } catch (Exception e) {
                // biz exception
                processor.handleException(channel, _request, SERVICE_ERROR, e);
                return;
            } finally {
                long elapsed = timerCtx.stop();

                // tracing recoding
                if (traceId != null && TracingUtil.isTracingNeeded()) {
                    TracingRecorder recorder = TracingUtil.getRecorder();
                    recorder.recording(PROVIDER, traceId.asText(), invokeId, callInfo, elapsed, channel);
                }
            }

            ResultWrapper result = new ResultWrapper();
            result.setResult(invokeResult);
            byte s_code = _request.serializerCode();
            Serializer serializer = SerializerFactory.getSerializer(s_code);
            byte[] bytes = serializer.writeObject(result);
            final int bodyLength = bytes.length;

            JResponseBytes response = new JResponseBytes(invokeId);
            response.status(Status.OK.value());
            response.bytes(s_code, bytes);
            channel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    long elapsed = SystemClock.millisClock().now() - _request.timestamp();

                    responseSizeHistogram.update(bodyLength);
                    processingTimer.update(elapsed, MILLISECONDS);

                    logger.debug("Service response[id: {}, length: {}] sent out, elapsed: {} millis.",
                            invokeId, bodyLength, elapsed);
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                    long elapsed = SystemClock.millisClock().now() - _request.timestamp();

                    logger.error("Service response[id: {}, length: {}] sent failed, elapsed: {} millis, {}, {}.",
                            invokeId, bodyLength, elapsed, channel, cause);
                }
            });
        } catch (Throwable t) {
            processor.handleException(channel, _request, SERVER_ERROR, t);
        }
    }
}
