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
import org.jupiter.rpc.Response;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.processor.task.RecyclableTask;
import org.jupiter.rpc.executor.ExecutorFactory;

import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.PROCESSOR_CORE_NUM_WORKERS;

/**
 * Default consumer's processor.
 *
 * jupiter
 * org.jupiter.rpc.consumer.processor
 *
 * @author jiachun.fjc
 */
public class DefaultConsumerProcessor implements ConsumerProcessor {

    private final Executor executor;

    public DefaultConsumerProcessor() {
        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.load(ConsumerExecutorFactory.class);
        executor = factory.newExecutor(PROCESSOR_CORE_NUM_WORKERS);
    }

    @Override
    public void handleResponse(JChannel ch, Response response) throws Exception {
        executor.execute(RecyclableTask.getInstance(ch, response));
    }
}
