package org.jupiter.rpc.consumer.invoker;

import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class SyncInvoker implements InvocationHandler {

    private final Dispatcher dispatcher;
    private final MessageWrapper message;

    public SyncInvoker(Dispatcher dispatcher, MessageWrapper message) {
        this.dispatcher = dispatcher;
        this.message = message;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        message.setMethodName(method.getName());
        message.setParameterTypes(method.getParameterTypes());
        message.setArgs(args);

        InvokeFuture result = dispatcher.dispatch(message);

        return result.singleResult();
    }
}
