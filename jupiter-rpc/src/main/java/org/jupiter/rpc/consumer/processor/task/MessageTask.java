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

package org.jupiter.rpc.consumer.processor.task;

import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.payload.JResponseBytes;

import static org.jupiter.serialization.SerializerHolder.serializerImpl;

/**
 *
 * jupiter
 * org.jupiter.rpc.consumer.processor.task
 *
 * @author jiachun.fjc
 */
public class MessageTask implements Runnable {

    private final JChannel channel;
    private final JResponse response;

    public MessageTask(JChannel channel, JResponse response) {
        this.channel = channel;
        this.response = response;
    }

    @Override
    public void run() {
        // stack copy
        final JResponse _response = response;
        final JResponseBytes _responseBytes = _response.responseBytes();

        byte s_code = _response.serializerCode();
        byte[] bytes = _responseBytes.bytes();
        _responseBytes.nullBytes();

        _response.result(serializerImpl(s_code).readObject(bytes, ResultWrapper.class));

        InvokeFuture.received(channel, _response);
    }
}
