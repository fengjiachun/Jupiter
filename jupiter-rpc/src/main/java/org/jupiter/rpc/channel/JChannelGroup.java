package org.jupiter.rpc.channel;

import org.jupiter.rpc.UnresolvedAddress;

/**
 * Based on the same address of the channel group.
 *
 * jupiter
 * org.jupiter.rpc.channel
 *
 * @author jiachun.fjc
 */
public interface JChannelGroup {

    /**
     * Returns the remote address of this group.
     */
    UnresolvedAddress remoteAddress();

    /**
     * Returns the next {@link JChannel} in the group.
     */
    JChannel next();

    /**
     * Returns <tt>true</tt> if this group contains no {@link JChannel}.
     */
    boolean isEmpty();

    /**
     * Adds the specified {@link JChannel} to this group.
     */
    boolean add(JChannel channel);

    /**
     * Removes the specified {@link JChannel} from this group.
     */
    boolean remove(JChannel channel);

    /**
     * Returns the number of {@link JChannel}s in this group (its cardinality).
     */
    int size();

    void waitForAvailable(long timeoutMillis);

    void setWeight(int weight);

    int getWeight();

    int getWarmUp();

    void setWarmUp(int warmUp);

    long getTimestamps();

    void resetTimestamps();
}
