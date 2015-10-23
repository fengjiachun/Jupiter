package org.jupiter.rpc.channel;

import java.net.SocketAddress;

/**
 * A nexus to a network socket or a component which is capable of I/O
 * operations such as read, write.
 *
 * jupiter
 * org.jupiter.rpc.channel
 *
 * @author jiachun.fjc
 */
public interface JChannel {

    /**
     * Returns the identifier of this {@link JChannel}.
     */
    String id();

    /**
     * Return {@code true} if the {@link JChannel} is active and so connected.
     */
    boolean isActive();

    /**
     * Return {@code true} if the current {@link Thread} is executed in the IO thread,
     * {@code false} otherwise.
     */
    boolean isIoThread();

    /**
     * Returns the local address where this channel is bound to.
     */
    SocketAddress localAddress();

    /**
     * Returns the remote address where this channel is connected to.
     */
    SocketAddress remoteAddress();

    /**
     * Returns {@code true} if and only if the I/O thread will perform the
     * requested write operation immediately.
     * Any write requests made when this method returns {@code false} are
     * queued until the I/O thread is ready to process the queued write requests.
     */
    boolean isWritable();

    /**
     * Request to close this {@link JChannel}.
     */
    JChannel close();

    /**
     * Request to close this {@link JChannel}.
     */
    JChannel close(JFutureListener<JChannel> listener);

    /**
     * Request to write a message on the channel.
     */
    JChannel write(Object msg);

    /**
     * Request to write a message on the channel.
     */
    JChannel write(Object msg, JFutureListener<JChannel> listener);
}
