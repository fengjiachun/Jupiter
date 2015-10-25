package org.jupiter.transport;

import org.jupiter.rpc.Directory;
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
     * Sets auto manage the connections
     */
    ConnectionManager manageConnections(Directory directory);

    /**
     * Server options [parent, child]
     */
    JConfig config();

    /**
     * Shutdown the server
     */
    void shutdownGracefully();

    interface ConnectionManager {

        void start();

        /**
         * Wait until the connections is available
         */
        void waitForAvailable(long timeoutMillis);
    }
}
