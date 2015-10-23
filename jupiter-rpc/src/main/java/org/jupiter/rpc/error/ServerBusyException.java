package org.jupiter.rpc.error;

/**
 * 抛出此异常是通常表明服务端的处理能力已经是饱和状态了
 *
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
