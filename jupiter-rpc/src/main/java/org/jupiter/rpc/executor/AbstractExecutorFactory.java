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

import java.util.concurrent.ThreadFactory;

import org.jupiter.common.concurrent.AffinityNamedThreadFactory;
import org.jupiter.common.concurrent.NamedThreadFactory;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.SystemPropertyUtil;

/**
 * jupiter
 * org.jupiter.rpc.executor
 *
 * @author jiachun.fjc
 */
public abstract class AbstractExecutorFactory implements ExecutorFactory {

    protected ThreadFactory threadFactory(String name) {
        boolean affinity = SystemPropertyUtil.getBoolean(EXECUTOR_AFFINITY_THREAD, false);
        if (affinity) {
            return new AffinityNamedThreadFactory(name);
        } else {
            return new NamedThreadFactory(name);
        }
    }

    protected int coreWorkers(Target target) {
        switch (target) {
            case CONSUMER:
                return SystemPropertyUtil.getInt(CONSUMER_EXECUTOR_CORE_WORKERS, JConstants.AVAILABLE_PROCESSORS << 1);
            case PROVIDER:
                return SystemPropertyUtil.getInt(PROVIDER_EXECUTOR_CORE_WORKERS, JConstants.AVAILABLE_PROCESSORS << 1);
            default:
                throw new IllegalArgumentException(String.valueOf(target));
        }
    }

    protected int maxWorkers(Target target) {
        switch (target) {
            case CONSUMER:
                return SystemPropertyUtil.getInt(CONSUMER_EXECUTOR_MAX_WORKERS, 32);
            case PROVIDER:
                return SystemPropertyUtil.getInt(PROVIDER_EXECUTOR_MAX_WORKERS, 512);
            default:
                throw new IllegalArgumentException(String.valueOf(target));
        }
    }

    protected int queueCapacity(Target target) {
        switch (target) {
            case CONSUMER:
                return SystemPropertyUtil.getInt(CONSUMER_EXECUTOR_QUEUE_CAPACITY, 32768);
            case PROVIDER:
                return SystemPropertyUtil.getInt(PROVIDER_EXECUTOR_QUEUE_CAPACITY, 32768);
            default:
                throw new IllegalArgumentException(String.valueOf(target));
        }
    }
}
