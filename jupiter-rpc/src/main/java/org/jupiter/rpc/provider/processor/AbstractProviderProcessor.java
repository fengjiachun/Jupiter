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
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.model.metadata.ResultWrapper;

import static org.jupiter.rpc.Status.SERVICE_ERROR;
import static org.jupiter.serialization.SerializerHolder.serializer;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public abstract class AbstractProviderProcessor implements ProviderProcessor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractProviderProcessor.class);

    @Override
    public void handleException(JChannel channel, JRequest request, Throwable cause) {
        ResultWrapper result = ResultWrapper.getInstance();
        result.setError(cause);

        JResponse response = new JResponse(request.invokeId());
        response.status(SERVICE_ERROR.value());
        response.bytes(serializer().writeObject(result));

        logger.error("An exception has been caught while processing request: {}.", result.getError());

        channel.write(response);
    }
}
