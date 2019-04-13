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
import org.jupiter.rpc.consumer.future.DefaultInvokeFutureGroup;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.jupiter.serialization.io.OutputBuf;
import org.jupiter.transport.CodecConfig;
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

    public DefaultBroadcastDispatcher(JClient client, SerializerType serializerType) {
        super(client, serializerType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> InvokeFuture<T> dispatch(JRequest request, Class<T> returnType) {
        // stack copy
        final Serializer _serializer = serializer();
        final MessageWrapper message = request.message();

        JChannelGroup[] groups = groups(message.getMetadata());
        JChannel[] channels = new JChannel[groups.length];
        for (int i = 0; i < groups.length; i++) {
            channels[i] = groups[i].next();
        }

        byte s_code = _serializer.code();
        // 在业务线程中序列化, 减轻IO线程负担
        boolean isLowCopy = CodecConfig.isCodecLowCopy();
        if (!isLowCopy) {
            byte[] bytes = _serializer.writeObject(message);
            request.bytes(s_code, bytes);
        }

        DefaultInvokeFuture<T>[] futures = new DefaultInvokeFuture[channels.length];
        for (int i = 0; i < channels.length; i++) {
            JChannel channel = channels[i];
            if (isLowCopy) {
                OutputBuf outputBuf =
                        _serializer.writeObject(channel.allocOutputBuf(), message);
                request.outputBuf(s_code, outputBuf);
            }
            futures[i] = write(channel, request, returnType, DispatchType.BROADCAST);
        }

        return DefaultInvokeFutureGroup.with(futures);
    }
}
