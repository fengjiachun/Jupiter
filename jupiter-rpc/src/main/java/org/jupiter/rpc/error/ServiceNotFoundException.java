package org.jupiter.rpc.error;

/**
 * For efficiency this exception will not have a stack trace.
 *
 * jupiter
 * org.jupiter.rpc.error
 *
 * @author jiachun.fjc
 */
public class ServiceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -2277731243490443074L;

    public ServiceNotFoundException() {}

    public ServiceNotFoundException(String message) {
        super(message);
    }

    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceNotFoundException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace()
    {
        return this;
    }
}
