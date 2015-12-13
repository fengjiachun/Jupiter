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

package org.jupiter.rpc.consumer.processor.task;

import org.jupiter.common.util.internal.Recyclers;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.model.metadata.ResultWrapper;

import static org.jupiter.serialization.SerializerHolder.serializer;

/**
 * Recyclable Task, reduce distribution and recovery of small objects (help gc).
 *
 * jupiter
 * org.jupiter.rpc.consumer.processor.task
 *
 * @author jiachun.fjc
 */
public class RecyclableTask implements Runnable {

    private JChannel channel;
    private JResponse response;

    @Override
    public void run() {
        try {
            // 在非IO线程里反序列化, 减轻IO线程负担
            response.result(serializer().readObject(response.bytes(), ResultWrapper.class));
            response.bytes(null);
            DefaultInvokeFuture.received(channel, response);
        } finally {
            recycle();
        }
    }

    public static RecyclableTask getInstance(JChannel channel, JResponse response) {
        RecyclableTask task = recyclers.get();

        task.channel = channel;
        task.response = response;
        return task;
    }

    private RecyclableTask(Recyclers.Handle<RecyclableTask> handle) {
        this.handle = handle;
    }

    private boolean recycle() {
        // help GC
        this.response = null;
        this.channel = null;

        return recyclers.recycle(this, handle);
    }

    private static final Recyclers<RecyclableTask> recyclers = new Recyclers<RecyclableTask>() {

        @Override
        protected RecyclableTask newObject(Handle<RecyclableTask> handle) {
            return new RecyclableTask(handle);
        }
    };

    private transient final Recyclers.Handle<RecyclableTask> handle;
}
