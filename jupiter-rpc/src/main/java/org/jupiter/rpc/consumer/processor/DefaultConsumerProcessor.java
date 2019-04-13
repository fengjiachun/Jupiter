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
package org.jupiter.rpc.consumer.processor;

import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.consumer.processor.task.MessageTask;
import org.jupiter.rpc.executor.CloseableExecutor;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.payload.JResponsePayload;
import org.jupiter.transport.processor.ConsumerProcessor;

/**
 * The default implementation of consumer's processor.
 *
 * jupiter
 * org.jupiter.rpc.consumer.processor
 *
 * @author jiachun.fjc
 */
public class DefaultConsumerProcessor implements ConsumerProcessor {

    private final CloseableExecutor executor;

    public DefaultConsumerProcessor() {
        this(ConsumerExecutors.executor());
    }

    public DefaultConsumerProcessor(CloseableExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void handleResponse(JChannel channel, JResponsePayload responsePayload) throws Exception {
        MessageTask task = new MessageTask(channel, new JResponse(responsePayload));
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }

    @Override
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
