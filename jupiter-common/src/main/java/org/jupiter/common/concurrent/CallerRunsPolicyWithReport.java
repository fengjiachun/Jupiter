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
 * 使用Caller-Runs(调用者执行)饱和策略, 不抛弃任务, 也不抛出异常, 而是将当前任务回退到调用者去执行, 从而降低新任务的流量.
 *
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class CallerRunsPolicyWithReport extends AbstractRejectedExecutionHandler {

    public CallerRunsPolicyWithReport(String threadPoolName) {
        super(threadPoolName, false, "");
    }

    public CallerRunsPolicyWithReport(String threadPoolName, String dumpPrefixName) {
        super(threadPoolName, true, dumpPrefixName);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        logger.error("Thread pool [{}] is exhausted! {}.", threadPoolName, e.toString());

        dumpJvmInfoIfNeeded();

        if (!e.isShutdown()) {
            r.run();
        }
    }
}
