package org.jupiter.rpc.consumer.future;

import java.util.concurrent.ExecutionException;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public interface JFuture {

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     */
    Object get() throws InterruptedException, ExecutionException;
}
