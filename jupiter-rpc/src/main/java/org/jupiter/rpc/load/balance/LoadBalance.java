package org.jupiter.rpc.load.balance;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Load balance.
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public interface LoadBalance<T> {

    /**
     * Select one in list
     */
    T select(CopyOnWriteArrayList<T> list);
}
