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

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.Status;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.model.metadata.ResultWrapper;

import static org.jupiter.serialization.SerializerHolder.serializerImpl;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public abstract class AbstractProviderProcessor implements ProviderProcessor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractProviderProcessor.class);

    @Override
    public void handleException(JChannel channel, JRequest request, Status status, Throwable cause) {
        ResultWrapper result = new ResultWrapper();
        result.setError(cause);

        logger.error("An exception has been caught while processing request: {}.", result.getError());

        byte s_code = request.serializerCode();
        byte[] bytes = serializerImpl(s_code).writeObject(result);
        JResponse response = JResponse.newInstance(request.invokeId(), s_code, status, bytes);
        channel.write(response);
    }
}
