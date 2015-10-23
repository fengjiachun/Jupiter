package org.jupiter.rpc.error;

/**
 * 请求内容反序列化失败时抛出此异常
 *
 * For efficiency this exception will not have a stack trace.
 *
 * jupiter
 * org.jupiter.rpc.error
 *
 * @author jiachun.fjc
 */
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = -6603241073638657127L;

    public BadRequestException() {}

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace()
    {
        return this;
    }
}
