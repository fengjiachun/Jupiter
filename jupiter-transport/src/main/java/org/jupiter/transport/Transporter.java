package org.jupiter.transport;

/**
 * jupiter
 * org.jupiter.transport
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
