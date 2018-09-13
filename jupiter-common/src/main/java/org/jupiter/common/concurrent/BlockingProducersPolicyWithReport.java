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

package org.jupiter.common.concurrent;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 使用阻塞生产者的饱和策略, 不抛弃任务, 也不抛出异常, 当队列满时改为调用BlockingQueue.put来实现生产者的阻塞.
 *
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class BlockingProducersPolicyWithReport extends AbstractRejectedExecutionHandler {

    public BlockingProducersPolicyWithReport(String threadPoolName) {
        super(threadPoolName, false, "");
    }

    public BlockingProducersPolicyWithReport(String threadPoolName, String dumpPrefixName) {
        super(threadPoolName, true, dumpPrefixName);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        logger.error("Thread pool [{}] is exhausted! {}.", threadPoolName, e.toString());

        dumpJvmInfoIfNeeded();

        if (!e.isShutdown()) {
            try {
                e.getQueue().put(r);
            } catch (InterruptedException ignored) { /* should not be interrupted */ }
        }
    }
}
