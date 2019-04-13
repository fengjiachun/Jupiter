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

import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.exception.JupiterSerializationException;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.io.InputBuf;
import org.jupiter.transport.CodecConfig;
import org.jupiter.transport.Status;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.payload.JResponsePayload;

/**
 * jupiter
 * org.jupiter.rpc.consumer.processor.task
 *
 * @author jiachun.fjc
 */
public class MessageTask implements Runnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

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
        final JResponsePayload _responsePayload = _response.payload();

        byte s_code = _response.serializerCode();

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        ResultWrapper wrapper;
        try {
            if (CodecConfig.isCodecLowCopy()) {
                InputBuf inputBuf = _responsePayload.inputBuf();
                wrapper = serializer.readObject(inputBuf, ResultWrapper.class);
            } else {
                byte[] bytes = _responsePayload.bytes();
                wrapper = serializer.readObject(bytes, ResultWrapper.class);
            }
            _responsePayload.clear();
        } catch (Throwable t) {
            logger.error("Deserialize object failed: {}, {}.", channel.remoteAddress(), StackTraceUtil.stackTrace(t));

            _response.status(Status.DESERIALIZATION_FAIL);
            wrapper = new ResultWrapper();
            wrapper.setError(new JupiterSerializationException(t));
        }
        _response.result(wrapper);

        DefaultInvokeFuture.received(channel, _response);
    }
}
