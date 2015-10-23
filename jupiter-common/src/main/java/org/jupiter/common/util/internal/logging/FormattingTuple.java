package org.jupiter.common.util.internal.logging;

/**
 * Holds the results of formatting done by {@link MessageFormatter}.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
class FormattingTuple {

    static final FormattingTuple NULL = new FormattingTuple(null);

    private final String message;
    private final Throwable throwable;
    private final Object[] argArray;

    FormattingTuple(String message) {
        this(message, null, null);
    }

    FormattingTuple(String message, Object[] argArray, Throwable throwable) {
        this.message = message;
        this.throwable = throwable;
        if (throwable == null) {
            this.argArray = argArray;
        } else {
            this.argArray = trimmedCopy(argArray);
        }
    }

    static Object[] trimmedCopy(Object[] argArray) {
        if (argArray == null || argArray.length == 0) {
            throw new IllegalStateException("empty or null argument array");
        }
        final int trimmedLen = argArray.length - 1;
        Object[] trimmed = new Object[trimmedLen];
        System.arraycopy(argArray, 0, trimmed, 0, trimmedLen);
        return trimmed;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgArray() {
        return argArray;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
