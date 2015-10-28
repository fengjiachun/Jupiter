package org.jupiter.transport;

import java.net.SocketAddress;

/**
 * Server acceptor.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JAcceptor<C, F> extends Transporter {

    /**
     * Local address.
     */
    SocketAddress localAddress();

    /**
     * Server options [parent, child].
     */
    JConfigGroup configGroup();

    /**
     * Create a new Channel and bind it.
     */
    C bind(SocketAddress address);

    /**
     * Start the server and wait until the server socket is closed.
     */
    void start() throws InterruptedException;

    /**
     * Start the server.
     */
    void start(boolean sync) throws InterruptedException;

    /**
     * Shutdown the server.
     */
    F shutdownGracefully();
}
