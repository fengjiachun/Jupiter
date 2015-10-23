package org.jupiter.transport;

import org.jupiter.common.util.Maps;
import org.jupiter.rpc.UnresolvedAddress;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class JConnectionManager {

    private static final ConcurrentMap<UnresolvedAddress, CopyOnWriteArrayList<JConnection>> connections = Maps.newConcurrentHashMap();

    public static void manage(JConnection connection) {
        UnresolvedAddress address = connection.getAddress();
        CopyOnWriteArrayList<JConnection> list = connections.get(address);
        if (list == null) {
            CopyOnWriteArrayList<JConnection> newList = new CopyOnWriteArrayList<>();
            list = connections.putIfAbsent(address, newList);
            if (list == null) {
                list = newList;
            }
        }
        list.add(connection);
    }

    public static void cancelReconnect(UnresolvedAddress address) {
        CopyOnWriteArrayList<JConnection> list = connections.get(address);
        if (list != null) {
            for (JConnection c : list) {
                c.setReconnect(false);
            }
        }
    }
}
