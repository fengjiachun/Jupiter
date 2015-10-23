package org.jupiter.rpc.error;

import org.jupiter.rpc.Status;

import java.net.SocketAddress;

/**
 * For efficiency this exception will not have a stack trace.
 *
 * jupiter
 * org.jupiter.rpc.error
 *
 * @author jiachun.fjc
 */
public class TimeoutException extends RemoteException {

    private static final long serialVersionUID = 8768621104391094458L;

    private final byte status;

    public TimeoutException(SocketAddress remoteAddress, byte status) {
        super(remoteAddress);
        this.status = status;
    }

    public TimeoutException(Throwable cause, SocketAddress remoteAddress, byte status) {
        super(cause, remoteAddress);
        this.status = status;
    }

    public TimeoutException(String message, SocketAddress remoteAddress, byte status) {
        super(message, remoteAddress);
        this.status = status;
    }

    public TimeoutException(String message, Throwable cause, SocketAddress remoteAddress, byte status) {
        super(message, cause, remoteAddress);
        this.status = status;
    }

    public Status status() {
        return Status.parse(status);
    }

    @Override
    public String toString() {
        return "TimeoutException{" +
                "status=" + status() +
                '}';
    }
}
