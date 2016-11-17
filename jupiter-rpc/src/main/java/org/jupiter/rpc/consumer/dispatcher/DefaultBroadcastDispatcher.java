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
import org.jupiter.rpc.channel.CopyOnWriteGroupList;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;

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
    public Object dispatch(JClient client, String methodName, Object[] args, Class<?> returnType) {
        // stack copy
        final ServiceMetadata _metadata = metadata;
        final Serializer _serializerImpl = serializerImpl;

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(client.appName());
        message.setMethodName(methodName);
        message.setArgs(args);

        CopyOnWriteGroupList groups = client.directory(_metadata);
        JChannel[] channels = new JChannel[groups.size()];
        InvokeFuture<?>[] futures = new InvokeFuture[channels.length];
        for (int i = 0; i < groups.size(); i++) {
            channels[i] = groups.get(i).next();
        }

        final JRequest request = JRequest.newInstance(_serializerImpl.code());
        request.message(message);
        // 在业务线程中序列化, 减轻IO线程负担
        request.bytes(_serializerImpl.writeObject(message));

        long timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        final ConsumerHook[] _hooks = getHooks();
        for (int i = 0; i < channels.length; i++) {
            JChannel ch = channels[i];
            final InvokeFuture<?> future = asFuture(ch, request, returnType, timeoutMillis)
                    .hooks(_hooks);
            futures[i] = future;

            ch.write(request, new JFutureListener<JChannel>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    future.setSentTime(); // 记录发送时间戳

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

                    InvokeFuture.received(
                            channel,
                            JResponse.newInstance(
                                    request.invokeId(),
                                    request.serializerCode(),
                                    CLIENT_ERROR,
                                    result
                            )
                    );
                }
            });
        }

        return futures;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected InvokeFuture<?> asFuture(JChannel channel, JRequest request, Class<?> returnType, long timeoutMillis) {
        return new InvokeFuture(channel, request, returnType, timeoutMillis, BROADCAST);
    }
}
