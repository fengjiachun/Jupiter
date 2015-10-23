package org.jupiter.common.concurrent.disruptor;

/**
 * jupiter
 * org.jupiter.common.concurrent.disruptor
 *
 * @author jiachun.fjc
 */
public class MessageEvent<T> {

    private T message;

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }
}
