package org.jupiter.rpc;

import org.jupiter.rpc.model.metadata.MessageWrapper;

import java.util.concurrent.atomic.AtomicLong;

import static org.jupiter.rpc.Status.*;

/**
 * Consumer请求数据
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class Request extends BytesHolder {

    private static final AtomicLong invokeIdGenerator = new AtomicLong(0);

    private final long invokeId;
    private MessageWrapper message; // 请求数据

    private transient long timestamps;
    private transient Status status = OK;

    public Request() {
        this(invokeIdGenerator.getAndIncrement());
    }

    public Request(long invokeId) {
        this.invokeId = invokeId;
    }

    public long invokeId() {
        return invokeId;
    }

    public MessageWrapper message() {
        return message;
    }

    public void message(MessageWrapper message) {
        this.message = message;
    }

    public long timestamps() {
        return timestamps;
    }

    public void timestamps(long timestamps) {
        this.timestamps = timestamps;
    }

    public Status status() {
        return status;
    }

    public void status(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Request{" +
                "invokeId=" + invokeId +
                ", message=" + message +
                '}';
    }
}
