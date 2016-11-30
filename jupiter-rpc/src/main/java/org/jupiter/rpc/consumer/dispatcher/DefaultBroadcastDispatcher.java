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

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.load.balance.LoadBalancer;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JChannelGroup;

import static org.jupiter.rpc.DispatchType.BROADCAST;

/**
 * 组播方式派发消息
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultBroadcastDispatcher extends AbstractDispatcher {

    public DefaultBroadcastDispatcher(
            LoadBalancer<JChannelGroup> loadBalancer, ServiceMetadata metadata, SerializerType serializerType) {
        super(loadBalancer, metadata, serializerType);
    }

    @Override
    public Object dispatch(JClient client, String methodName, Object[] args, Class<?> returnType) {
        // stack copy
        final ServiceMetadata _metadata = getMetadata();
        final Serializer _serializer = getSerializer();

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(client.appName());
        message.setMethodName(methodName);
        message.setArgs(args);

        CopyOnWriteGroupList groups = client.connector().directory(_metadata);
        JChannel[] channels = new JChannel[groups.size()];
        InvokeFuture<?>[] futures = new InvokeFuture[channels.length];
        for (int i = 0; i < groups.size(); i++) {
            channels[i] = groups.get(i).next();
        }

        JRequest request = new JRequest();
        request.serializerCode(_serializer.code());
        request.message(message);
        // 在业务线程中序列化, 减轻IO线程负担
        request.bytes(_serializer.writeObject(message));

        long timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        for (int i = 0; i < channels.length; i++) {
            JChannel ch = channels[i];
            InvokeFuture<?> future = asFuture(ch, request, returnType, timeoutMillis)
                    .setHooks(getHooks());
            futures[i] = write(ch, request, future);
        }

        return futures;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected InvokeFuture<?> asFuture(JChannel channel, JRequest request, Class<?> returnType, long timeoutMillis) {
        return new InvokeFuture(channel, request, returnType, timeoutMillis, BROADCAST);
    }
}
