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

package org.jupiter.rpc.provider.processor;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.executor.ExecutorFactory;
import org.jupiter.rpc.provider.processor.task.RecyclableTask;

import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.PROCESSOR_CORE_NUM_WORKERS;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public class DefaultProviderProcessor extends AbstractProviderProcessor {

    private final Executor executor;

    public DefaultProviderProcessor(JServer jServer) {
        super(jServer);

        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.load(ProviderExecutorFactory.class);
        executor = factory.newExecutor(PROCESSOR_CORE_NUM_WORKERS);
    }

    public DefaultProviderProcessor(JServer jServer, Executor executor) {
        super(jServer);
        this.executor = executor;
    }

    @Override
    public void handleRequest(final JChannel ch, final Request request) throws Exception {
        // 1. 反序列化相对较耗cpu, 避免在IO线程中执行.
        // 2. 根据Java Flight Recordings (JFR) 观察结果, protostuff在发序列化时,
        //      io.protostuff.runtime.RuntimeEnv.loadClass有较多的锁竞争, 避免在IO线程中执行.
        executor.execute(RecyclableTask.getInstance(this, ch, request));
    }
}
