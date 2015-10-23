package org.jupiter.rpc.error;

/**
 * For efficiency this exception will not have a stack trace.
 *
 * jupiter
 * org.jupiter.rpc.error
 *
 * @author jiachun.fjc
 */
public class ServerBusyException extends RuntimeException {

    private static final long serialVersionUID = 4812626729436624336L;

    public ServerBusyException() {}

    public ServerBusyException(String message) {
        super(message);
    }

    public ServerBusyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerBusyException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace()
    {
        return this;
    }
}
