package org.jupiter.rpc.consumer.future;

import org.jupiter.rpc.JListener;
import org.jupiter.rpc.aop.ConsumerHook;

import java.util.List;

/**
 * Invoke future.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public interface InvokeFuture {

    /**
     * Sets hooks for consumer
     */
    InvokeFuture hooks(List<ConsumerHook> hooks);

    /**
     * Sets listener for asynchronous rpc
     */
    InvokeFuture listener(JListener listener);

    /**
     * Sets send time
     */
    void sent();

    /**
     * Returns the result for rpc
     */
    Object singleResult() throws Throwable;
}
