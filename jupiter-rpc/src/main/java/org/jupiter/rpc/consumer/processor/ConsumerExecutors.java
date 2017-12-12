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
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.rpc.executor.ExecutorFactory;

import java.util.concurrent.Executor;

/**
 * jupiter
 * org.jupiter.rpc.consumer.processor
 *
 * @author jiachun.fjc
 */
public class ConsumerExecutors {

    private static final Executor executor;

    static {
        String factoryName = SystemPropertyUtil.get("jupiter.executor.factory.consumer.factory_name");
        ExecutorFactory factory;
        if (Strings.isNullOrEmpty(factoryName)) {
            factory = (ExecutorFactory) JServiceLoader.load(ConsumerExecutorFactory.class)
                    .first();
        } else {
            factory = (ExecutorFactory) JServiceLoader.load(ConsumerExecutorFactory.class)
                    .find(factoryName);
        }

        executor = factory.newExecutor(ExecutorFactory.Target.CONSUMER, "jupiter-consumer-processor");
    }

    public static Executor executor() {
        return executor;
    }

    public static void execute(Runnable command) {
        executor.execute(command);
    }
}
