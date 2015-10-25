package org.jupiter.rpc;

import org.jupiter.registry.RegisterMeta;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class UnresolvedAddress {

    private final String host;
    private final int port;

    public static UnresolvedAddress cast(RegisterMeta.Address address) {
        return new UnresolvedAddress(address.getHost(), address.getPort());
    }

    public UnresolvedAddress(String host, int port) {
        checkNotNull(host, "host can't be null");
        checkArgument(port > 0 && port < 0xFFFF, "port out of range:" + port);

        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnresolvedAddress that = (UnresolvedAddress) o;

        return port == that.port && host.equals(that.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return host + ':' + port;
    }
}
