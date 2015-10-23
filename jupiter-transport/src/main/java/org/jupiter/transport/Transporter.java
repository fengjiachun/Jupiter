package org.jupiter.transport;

/**
 * jupiter
 * org.jupiter.transport.api
 *
 * @author jiachun.fjc
 */
public interface Transporter {

    /**
     * Transport protocol
     */
    Protocol protocol();

    /**
     * Transport protocol
     */
    enum Protocol {
        TCP,
        UDT
    }
}
