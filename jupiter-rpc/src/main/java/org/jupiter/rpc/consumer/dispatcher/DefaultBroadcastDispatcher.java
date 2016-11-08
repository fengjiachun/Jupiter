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

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.channel.CopyOnWriteGroupList;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.promise.DefaultInvokePromise;
import org.jupiter.rpc.consumer.promise.InvokePromise;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.SerializerType;

import java.util.List;

import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.Status.CLIENT_ERROR;

/**
 * 组播方式派发消息
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultBroadcastDispatcher extends AbstractDispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultBroadcastDispatcher.class);

    public DefaultBroadcastDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        super(metadata, serializerType);
    }

    @Override
    public InvokePromise<?> dispatch(JClient client, String methodName, Object[] args) {
        final ServiceMetadata _metadata = metadata; // stack copy

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(client.appName());
        message.setMethodName(methodName);
        message.setArgs(args);

        CopyOnWriteGroupList groups = client.directory(_metadata);
        List<JChannel> channels = Lists.newArrayListWithCapacity(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            channels.add(groups.get(i).next());
        }

        final JRequest request = JRequest.newInstance(serializerType.value());
        request.message(message);
        // 在业务线程中序列化, 减轻IO线程负担
        request.bytes(serializerImpl.writeObject(message));

        long timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        final ConsumerHook[] _hooks = getHooks();
        JListener _listener = getListener();
        for (JChannel ch : channels) {
            final InvokePromise<?> promise = asPromise(ch, request, timeoutMillis)
                    .hooks(_hooks)
                    .listener(_listener);

            ch.write(request, new JFutureListener<JChannel>() {

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
        }

        return null;
    }

    @Override
    protected InvokePromise<?> asPromise(JChannel channel, JRequest request, long timeoutMillis) {
        return new DefaultInvokePromise(channel, request, timeoutMillis, BROADCAST);
    }
}
