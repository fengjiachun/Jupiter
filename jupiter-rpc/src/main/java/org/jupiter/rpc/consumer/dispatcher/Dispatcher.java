package org.jupiter.rpc.consumer.dispatcher;

import org.jupiter.rpc.JListener;
import org.jupiter.rpc.aop.ConsumerHook;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.consumer.future.InvokeFuture;

import java.util.List;

/**
 * Dispatcher for consumer
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public interface Dispatcher {

    InvokeFuture dispatch(MessageWrapper message);

    List<ConsumerHook> getHooks();

    void setHooks(List<ConsumerHook> hooks);

    JListener getListener();

    void setListener(JListener listener);

    int getTimeoutMills();

    void setTimeoutMills(int timeoutMills);
}
