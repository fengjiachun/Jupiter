package org.jupiter.common.util.internal.logging;

/**
 * The log level that {@link InternalLogger} can log at.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
public enum InternalLogLevel {

    /**
     * 'TRACE' log level.
     */
    TRACE,
    /**
     * 'DEBUG' log level.
     */
    DEBUG,
    /**
     * 'INFO' log level.
     */
    INFO,
    /**
     * 'WARN' log level.
     */
    WARN,
    /**
     * 'ERROR' log level.
     */
    ERROR
}
