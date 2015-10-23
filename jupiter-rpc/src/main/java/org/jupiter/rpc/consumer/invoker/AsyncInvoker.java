package org.jupiter.rpc.consumer.invoker;

import org.jupiter.common.util.Reflects;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class AsyncInvoker implements InvocationHandler {

    private final Dispatcher dispatcher;
    private final MessageWrapper message;

    public AsyncInvoker(Dispatcher dispatcher, MessageWrapper message) {
        this.dispatcher = dispatcher;
        this.message = message;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        message.setMethodName(method.getName());
        message.setParameterTypes(method.getParameterTypes());
        message.setArgs(args);

        dispatcher.dispatch(message);

        return Reflects.getTypeDefaultValue(method.getReturnType());
    }
}
