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

import org.jupiter.rpc.ConsumerHook;
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.consumer.future.DefaultInvokeFutureGroup;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JChannelGroup;

/**
 * 组播方式派发消息.
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultBroadcastDispatcher extends AbstractDispatcher {

    public DefaultBroadcastDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        super(metadata, serializerType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> InvokeFuture<T> dispatch(JClient client, String methodName, Object[] args, Class<T> returnType) {
        // stack copy
        final ServiceMetadata _metadata = metadata();
        final Serializer _serializer = serializer();

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(client.appName());
        message.setMethodName(methodName);
        // 不需要方法参数类型, 服务端会根据args具体类型按照JLS规则动态dispatch
        message.setArgs(args);

        JChannelGroup[] groups = client
                .connector()
                .directory(_metadata)
                .snapshot();
        JChannel[] channels = new JChannel[groups.length];
        for (int i = 0; i < groups.length; i++) {
            channels[i] = groups[i].next();
        }

        byte s_code = _serializer.code();
        // 在业务线程中序列化, 减轻IO线程负担
        byte[] bytes = _serializer.writeObject(message);

        JRequest request = new JRequest();
        request.message(message);
        request.bytes(s_code, bytes);

        long invokeId = request.invokeId();
        ConsumerHook[] hooks = hooks();
        InvokeFuture<T>[] futures = new DefaultInvokeFuture[channels.length];
        long timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        for (int i = 0; i < channels.length; i++) {
            JChannel ch = channels[i];
            DefaultInvokeFuture<T> future = DefaultInvokeFuture
                    .with(invokeId, ch, returnType, timeoutMillis, DispatchType.BROADCAST)
                    .hooks(hooks);
            futures[i] = write(ch, request, future, DispatchType.BROADCAST);
        }

        return DefaultInvokeFutureGroup.with(futures);
    }
}
