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
import org.jupiter.common.util.SpiMetadata;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

/**
 * Provide a disruptor implementation of executor.
 *
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
@SpiMetadata(name = "disruptor")
public class DisruptorExecutorFactory extends AbstractExecutorFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DisruptorExecutorFactory.class);

    @Override
    public CloseableExecutor newExecutor(Target target, String name) {
        final TaskDispatcher executor = new TaskDispatcher(
                coreWorkers(target),
                threadFactory(name),
                queueCapacity(target),
                maxWorkers(target),
                waitStrategyType(target, WaitStrategyType.LITE_BLOCKING_WAIT),
                "jupiter");

        return new CloseableExecutor() {

            @Override
            public void execute(Runnable r) {
                executor.execute(r);
            }

            @Override
            public void shutdown() {
                logger.warn("DisruptorExecutorFactory#{} shutdown.", executor);
                executor.shutdown();
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    private WaitStrategyType waitStrategyType(Target target, WaitStrategyType defaultType) {
        WaitStrategyType strategyType = null;
        switch (target) {
            case CONSUMER:
                strategyType = WaitStrategyType.parse(SystemPropertyUtil.get(CONSUMER_DISRUPTOR_WAIT_STRATEGY_TYPE));
                break;
            case PROVIDER:
                strategyType = WaitStrategyType.parse(SystemPropertyUtil.get(PROVIDER_DISRUPTOR_WAIT_STRATEGY_TYPE));
                break;
        }

        return strategyType == null ? defaultType : strategyType;
    }
}
