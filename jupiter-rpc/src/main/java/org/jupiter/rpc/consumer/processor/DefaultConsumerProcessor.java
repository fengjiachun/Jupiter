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

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.processor.task.MessageTask;
import org.jupiter.rpc.executor.ExecutorFactory;

import java.util.concurrent.Executor;

import static org.jupiter.rpc.executor.ExecutorFactory.Target;

/**
 * The default implementation of consumer's processor.
 *
 * jupiter
 * org.jupiter.rpc.consumer.processor
 *
 * @author jiachun.fjc
 */
public class DefaultConsumerProcessor implements ConsumerProcessor {

    private final Executor executor;

    public DefaultConsumerProcessor() {
        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.loadFirst(ConsumerExecutorFactory.class);
        executor = factory.newExecutor(Target.CONSUMER);
    }

    @Override
    public void handleResponse(JChannel channel, JResponse response) throws Exception {
        MessageTask task = new MessageTask(channel, response);
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }
}
