package org.jupiter.transport;

import org.jupiter.rpc.UnresolvedAddress;

/**
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JConnector<C> extends Transporter {

    /**
     * Connect to the remote peer.
     */
    C connect(UnresolvedAddress remoteAddress);

    /**
     * Connect to the remote peer.
     */
    C connect(UnresolvedAddress remoteAddress, boolean async);

    /**
     * Server options [parent, child]
     */
    JConfig config();

    /**
     * Shutdown the server
     */
    void shutdownGracefully();
}
