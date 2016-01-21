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
import org.jupiter.rpc.*;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import static org.jupiter.rpc.Status.CLIENT_ERROR;
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
    public InvokeFuture dispatch(JClient proxy, String methodName, Object[] args) {
        // stack copy
        final ServiceMetadata _metadata = metadata;

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(proxy.appName());
        message.setMethodName(methodName);
        message.setArgs(args);
        // tracing [允许在业务代码里设置traceId]
        String traceId = Tracing.getCurrent();
        if (traceId == null) {
            traceId = Tracing.generateTraceId();
        } else {
            Tracing.setCurrent(null);
        }
        message.setTraceId(traceId);

        JChannel channel = proxy.select(_metadata);

        final JRequest request = new JRequest();
        request.message(message);
        request.bytes(serializerImpl().writeObject(message));
        final ConsumerHook[] _hooks = getHooks();
        final InvokeFuture invokeFuture = asInvokeFuture(channel, request)
                .hooks(_hooks)
                .listener(getListener());

        channel.write(request, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                invokeFuture.setSentOutTimestamp();

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

                JResponse response = JResponse.getInstance(request.invokeId(), CLIENT_ERROR, result);
                DefaultInvokeFuture.received(channel, response);
            }
        });

        return invokeFuture;
    }

    @Override
    protected InvokeFuture asInvokeFuture(JChannel channel, JRequest request) {
        return new DefaultInvokeFuture(channel, request, getTimeoutMills());
    }
}
