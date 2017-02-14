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

package org.jupiter.rpc.provider.processor;

import org.jupiter.common.util.ExceptionUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.provider.LookupService;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.transport.Status;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JFutureListener;
import org.jupiter.transport.payload.JRequestBytes;
import org.jupiter.transport.payload.JResponseBytes;
import org.jupiter.transport.processor.ProviderProcessor;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public abstract class AbstractProviderProcessor implements
        ProviderProcessor, LookupService, FlowController<JRequest> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractProviderProcessor.class);

    @Override
    public void handleException(JChannel channel, JRequestBytes request, Status status, Throwable cause) {
        handleException(channel, request.invokeId(), request.serializerCode(), status.value(), cause);
    }

    public void handleException(JChannel channel, JRequest request, Status status, Throwable cause) {
        handleException(channel, request.invokeId(), request.serializerCode(), status.value(), cause);
    }

    private void handleException(JChannel channel, long invokeId, byte s_code, byte status, Throwable cause) {
        logger.error(
                "An exception has been caught while processing request: {}, {}, {}.",
                invokeId, channel.remoteAddress(), stackTrace(cause)
        );

        ResultWrapper result = new ResultWrapper();
        // 截断cause, 避免客户端无法找到cause类型而无法序列化
        cause = ExceptionUtil.cutCause(cause);
        result.setError(cause);

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        byte[] bytes = serializer.writeObject(result);

        JResponseBytes response = new JResponseBytes(invokeId);
        response.status(status);
        response.bytes(s_code, bytes);

        channel.write(response, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                logger.debug("Service error message sent out: {}.", channel);
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                logger.warn("Service error message sent failed: {}, {}.", channel, stackTrace(cause));
            }
        });
    }
}
