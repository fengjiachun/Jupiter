package org.jupiter.rpc.consumer.future;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public interface JFuture {

    /**
     * Returns the result of rpc.
     */
    Object getResult() throws Throwable;
}
