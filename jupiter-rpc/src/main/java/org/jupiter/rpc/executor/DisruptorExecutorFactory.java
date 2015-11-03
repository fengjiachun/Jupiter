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

package org.jupiter.rpc.executor;

import org.jupiter.common.concurrent.disruptor.TaskDispatcher;
import org.jupiter.common.concurrent.disruptor.WaitStrategyType;

import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.PROCESSOR_MAX_NUM_WORKS;
import static org.jupiter.common.util.JConstants.PROCESSOR_WORKER_QUEUE_CAPACITY;

/**
 * Disruptor executor factory.
 *
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
public class DisruptorExecutorFactory implements ExecutorFactory {

    @Override
    public Executor newExecutor(int parallelism, Object... args) {
        WaitStrategyType waitStrategyType = null;
        if (args.length > 0 && args[0] instanceof WaitStrategyType) {
            waitStrategyType = (WaitStrategyType) args[0];
        }
        if (waitStrategyType == null) {
            waitStrategyType = WaitStrategyType.LITE_BLOCKING_WAIT;
        }

        return new TaskDispatcher(
                parallelism,
                "processor",
                PROCESSOR_WORKER_QUEUE_CAPACITY,
                PROCESSOR_MAX_NUM_WORKS,
                waitStrategyType);
    }
}
