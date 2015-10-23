package org.jupiter.rpc.consumer.processor.task;

import org.jupiter.common.util.internal.Recyclers;
import org.jupiter.rpc.Response;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.serialization.SerializerHolder;

/**
 * jupiter
 * org.jupiter.rpc.consumer.processor.task
 *
 * @author jiachun.fjc
 */
public class RecyclableTask implements Runnable {

    private JChannel jChannel;
    private Response response;

    @Override
    public void run() {
        try {
            // 业务线程里反序列化, 减轻IO线程负担
            response.result(SerializerHolder.getSerializer().readObject(response.bytes(), ResultWrapper.class));
            response.bytes(null);
            DefaultInvokeFuture.received(jChannel, response);
        } finally {
            recycle();
        }
    }

    public static RecyclableTask getInstance(JChannel jChannel, Response response) {
        RecyclableTask task = recyclers.get();

        task.jChannel = jChannel;
        task.response = response;
        return task;
    }

    private RecyclableTask(Recyclers.Handle<RecyclableTask> handle) {
        this.handle = handle;
    }

    private boolean recycle() {
        // help GC
        this.response = null;
        this.jChannel = null;

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
