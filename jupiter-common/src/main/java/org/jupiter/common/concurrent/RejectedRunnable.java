package org.jupiter.common.concurrent;

/**
 * jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public interface RejectedRunnable extends Runnable {

    void reject();
}
