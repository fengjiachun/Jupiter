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
package org.jupiter.common.concurrent.disruptor;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.TimeoutHandler;
import com.lmax.disruptor.WorkHandler;

/**
 * Callback interface to be implemented for processing events as they become available in the RingBuffer.
 *
 * jupiter
 * org.jupiter.common.concurrent.disruptor
 *
 * @author jiachun.fjc
 */
public class TaskHandler implements
        EventHandler<MessageEvent<Runnable>>, WorkHandler<MessageEvent<Runnable>>, TimeoutHandler, LifecycleAware {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TaskHandler.class);

    @Override
    public void onEvent(MessageEvent<Runnable> event, long sequence, boolean endOfBatch) throws Exception {
        event.getMessage().run();
    }

    @Override
    public void onEvent(MessageEvent<Runnable> event) throws Exception {
        event.getMessage().run();
    }

    @Override
    public void onTimeout(long sequence) throws Exception {
        if (logger.isWarnEnabled()) {
            logger.warn("Task timeout on: {}, sequence: {}.", Thread.currentThread().getName(), sequence);
        }
    }

    @Override
    public void onStart() {
        if (logger.isWarnEnabled()) {
            logger.warn("Task handler on start: {}.", Thread.currentThread().getName());
        }
    }

    @Override
    public void onShutdown() {
        if (logger.isWarnEnabled()) {
            logger.warn("Task handler on shutdown: {}.", Thread.currentThread().getName());
        }
    }
}
