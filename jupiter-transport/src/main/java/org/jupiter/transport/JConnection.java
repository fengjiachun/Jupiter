package org.jupiter.transport;

import org.jupiter.rpc.UnresolvedAddress;

/**
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public abstract class JConnection {

    private final UnresolvedAddress address;

    public JConnection(UnresolvedAddress address) {
        this.address = address;
    }

    public UnresolvedAddress getAddress() {
        return address;
    }

    public abstract void setReconnect(boolean reconnect);
}
