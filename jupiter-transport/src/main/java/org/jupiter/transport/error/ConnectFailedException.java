package org.jupiter.transport.error;

/**
 * jupiter
 * org.jupiter.transport.error
 *
 * @author jiachun.fjc
 */
public class ConnectFailedException extends RuntimeException {

    private static final long serialVersionUID = -2890742743547564900L;

    public ConnectFailedException() {
        super();
    }

    public ConnectFailedException(String message) {
        super(message);
    }

    public ConnectFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectFailedException(Throwable cause) {
        super(cause);
    }
}
