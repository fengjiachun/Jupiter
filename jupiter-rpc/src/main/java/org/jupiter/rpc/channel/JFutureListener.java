package org.jupiter.rpc.channel;

import java.util.EventListener;

/**
 * Listener for {@link JChannel} event.
 *
 * jupiter
 * org.jupiter.rpc.channel
 *
 * @author jiachun.fjc
 */
public interface JFutureListener<T> extends EventListener {

    void operationComplete(T ch, boolean isSuccess) throws Exception;
}
