package org.jupiter.rpc.error;

/**
 * 服务端限流时抛出磁异常
 *
 * For efficiency this exception will not have a stack trace.
 *
 * jupiter
 * org.jupiter.rpc.error
 *
 * @author jiachun.fjc
 */
public class TPSLimitException extends RuntimeException {

    private static final long serialVersionUID = 3478741195763320940L;

    public TPSLimitException() {}

    public TPSLimitException(String message) {
        super(message);
    }

    public TPSLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    public TPSLimitException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace()
    {
        return this;
    }
}
