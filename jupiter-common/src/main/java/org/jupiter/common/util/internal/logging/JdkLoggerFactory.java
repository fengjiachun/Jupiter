package org.jupiter.common.util.internal.logging;

import java.util.logging.Logger;

/**
 * Logger factory which creates a
 * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/logging/">java.util.logging</a>
 * logger.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
public class JdkLoggerFactory extends InternalLoggerFactory {

    @Override
    public InternalLogger newInstance(String name) {
        return new JdkLogger(Logger.getLogger(name));
    }
}
