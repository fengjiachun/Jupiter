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

import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.load.balance.LoadBalancer;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.channel.JChannel;

/**
 * 单播方式派发消息.
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultRoundDispatcher extends AbstractDispatcher {

    public DefaultRoundDispatcher(
            LoadBalancer loadBalancer, ServiceMetadata metadata, SerializerType serializerType) {
        super(loadBalancer, metadata, serializerType);
    }

    @Override
    public <T> InvokeFuture<T> dispatch(JClient client, String methodName, Object[] args, Class<T> returnType) {
        // stack copy
        final Serializer _serializer = serializer();

        MessageWrapper message = new MessageWrapper(metadata());
        message.setAppName(client.appName());
        message.setMethodName(methodName);
        // 不需要方法参数类型, 服务端会根据args具体类型按照JLS规则动态dispatch
        message.setArgs(args);

        // 通过软负载均衡选择一个channel
        JChannel channel = select(client, message);

        doTracing(message, methodName, channel);

        byte s_code = _serializer.code();
        // 在业务线程中序列化, 减轻IO线程负担
        byte[] bytes = _serializer.writeObject(message);

        JRequest request = new JRequest();
        request.message(message);
        request.bytes(s_code, bytes);

        long timeoutMillis = methodSpecialTimeoutMillis(methodName);
        DefaultInvokeFuture<T> future = DefaultInvokeFuture
                .with(request.invokeId(), channel, returnType, timeoutMillis, DispatchType.ROUND)
                .hooks(hooks());

        return write(channel, request, future, DispatchType.ROUND);
    }
}
