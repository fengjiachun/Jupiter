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

import static org.jupiter.rpc.Status.CLIENT_ERROR;
import static org.jupiter.rpc.tracing.TracingRecorder.Role.CONSUMER;
import static org.jupiter.serialization.SerializerHolder.serializerImpl;

/**
 * 单播方式派发消息
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultRoundDispatcher extends AbstractDispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultRoundDispatcher.class);

    public DefaultRoundDispatcher(ServiceMetadata metadata) {
        super(metadata);
    }

    @Override
    public InvokePromise dispatch(JClient client, String methodName, Object[] args) {
        final ServiceMetadata _metadata = metadata; // stack copy

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(client.appName());
        message.setMethodName(methodName);
        message.setArgs(args);

        JChannel channel = client.select(_metadata);
        final JRequest request = new JRequest();

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
        request.bytes(serializerImpl().writeObject(message));

        int timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        final ConsumerHook[] _hooks = getHooks();
        final InvokePromise promise = asPromise(channel, request, timeoutMillis)
                .hooks(_hooks)
                .listener(getListener());

        channel.write(request, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                promise.chalkUpSentTimestamp();

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

                JResponse response = JResponse.newInstance(request.invokeId(), CLIENT_ERROR, result);
                DefaultInvokePromise.received(channel, response);
            }
        });

        return promise;
    }

    @Override
    protected InvokePromise asPromise(JChannel channel, JRequest request, int timeoutMillis) {
        return new DefaultInvokePromise(channel, request, timeoutMillis);
    }
}
