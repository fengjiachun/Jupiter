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

package org.jupiter.rpc.consumer.dispatcher;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.ConsumerHook;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.promise.DefaultInvokePromise;
import org.jupiter.rpc.consumer.promise.InvokePromise;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingEye;
import org.jupiter.rpc.tracing.TracingRecorder;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;

import static org.jupiter.rpc.Status.CLIENT_ERROR;
import static org.jupiter.rpc.tracing.TracingRecorder.Role.CONSUMER;

/**
 * 单播方式派发消息, 仅支持异步回调, 不支持同步调用.
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultRoundDispatcher extends AbstractDispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultRoundDispatcher.class);

    public DefaultRoundDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        super(metadata, serializerType);
    }

    @Override
    public InvokePromise<?> dispatch(JClient client, String methodName, Object[] args) {
        // stack copy
        final ServiceMetadata _metadata = metadata;
        final Serializer _serializerImpl = serializerImpl;

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(client.appName());
        message.setMethodName(methodName);
        message.setArgs(args);

        // 通过软负载选择一个channel
        JChannel channel = client.select(_metadata);
        final JRequest request = JRequest.newInstance(_serializerImpl.code());

        // tracing
        if (TracingEye.isTracingNeeded()) {
            TraceId traceId = TracingEye.getCurrent();
            if (traceId == null) {
                traceId = TraceId.newInstance(TracingEye.generateTraceId());
            }
            message.setTraceId(traceId);

            TracingRecorder recorder = TracingEye.getRecorder();
            recorder.recording(CONSUMER, traceId.asText(), request.invokeId(), _metadata.directory(), methodName, channel);
        }

        request.message(message);
        // 在业务线程中序列化, 减轻IO线程负担
        request.bytes(_serializerImpl.writeObject(message));

        long timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        final ConsumerHook[] _hooks = getHooks();
        final InvokePromise<?> promise = asPromise(channel, request, timeoutMillis)
                .hooks(_hooks)
                .listener(getListener());

        channel.write(request, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                promise.chalkUpSentTimestamp(); // 记录发送时间戳

                // hook.before()
                if (_hooks != null) {
                    for (ConsumerHook h : _hooks) {
                        h.before(request, channel);
                    }
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                logger.warn("Writes {} fail on {}, {}.", request, channel, cause);

                ResultWrapper result = new ResultWrapper();
                result.setError(cause);

                JResponse response = JResponse.newInstance(
                        request.invokeId(), request.serializerCode(), CLIENT_ERROR, result);
                DefaultInvokePromise.received(channel, response);
            }
        });

        return promise;
    }

    @Override
    protected InvokePromise<?> asPromise(JChannel channel, JRequest request, long timeoutMillis) {
        return new DefaultInvokePromise(channel, request, timeoutMillis);
    }
}
