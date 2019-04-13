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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 如果当前任务实现了 {@link RejectedRunnable} 接口, 那么交给用户去实现拒绝任务的逻辑,
 * 否则以FIFO的方式抛弃队列中一部分现有任务.
 *
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class RejectedTaskPolicyWithReport extends AbstractRejectedExecutionHandler {

    public RejectedTaskPolicyWithReport(String threadPoolName) {
        super(threadPoolName, false, "");
    }

    public RejectedTaskPolicyWithReport(String threadPoolName, String dumpPrefixName) {
        super(threadPoolName, true, dumpPrefixName);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        logger.error("Thread pool [{}] is exhausted! {}.", threadPoolName, e.toString());

        dumpJvmInfoIfNeeded();

        if (r instanceof RejectedRunnable) {
            ((RejectedRunnable) r).rejected(); // 交给用户来处理
        } else {
            if (!e.isShutdown()) {
                BlockingQueue<Runnable> queue = e.getQueue();
                int discardSize = queue.size() >> 1;
                for (int i = 0; i < discardSize; i++) {
                    queue.poll();
                }

                try {
                    queue.put(r);
                } catch (InterruptedException ignored) { /* should not be interrupted */ }
            }
        }
    }
}
