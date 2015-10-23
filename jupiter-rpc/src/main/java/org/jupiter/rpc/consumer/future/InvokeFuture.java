package org.jupiter.rpc.consumer.future;

import org.jupiter.rpc.JListener;
import org.jupiter.rpc.aop.ConsumerHook;

import java.util.List;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public interface InvokeFuture {

    InvokeFuture hooks(List<ConsumerHook> hooks);

    InvokeFuture listener(JListener listener);

    void sent();

    Object singleResult() throws Throwable;
}
