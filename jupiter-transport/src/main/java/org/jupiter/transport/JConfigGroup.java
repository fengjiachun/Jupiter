package org.jupiter.transport;

/**
 * Config group (parent config and child config)
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JConfigGroup {

    /**
     * Parent config
     */
    JConfig parent();

    /**
     * Child config
     */
    JConfig child();
}
