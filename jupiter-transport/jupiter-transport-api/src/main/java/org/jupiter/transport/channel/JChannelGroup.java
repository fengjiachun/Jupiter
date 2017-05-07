/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.transport.channel;

import org.jupiter.transport.Directory;
import org.jupiter.transport.UnresolvedAddress;

import java.util.List;

/**
 * Based on the same address of the channel group.
 *
 * 要注意的是它管理的是相同地址的 {@link JChannel}.
 *
 * jupiter
 * org.jupiter.transport.channel
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
     * Returns all {@link JChannel}s in the group.
     */
    List<? extends JChannel> channels();

    /**
     * Returns true if this group contains no {@link JChannel}.
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

    /**
     * Sets the capacity of this group.
     */
    void setCapacity(int capacity);

    /**
     * The capacity of this group.
     */
    int getCapacity();

    /**
     * If available return true, otherwise return false.
     */
    boolean isAvailable();

    /**
     * Wait until the {@link JChannel}s are available or timeout,
     * if available return true, otherwise return false.
     */
    boolean waitForAvailable(long timeoutMillis);

    /**
     * Weight of service.
     */
    int getWeight(Directory directory);

    /**
     * Sets the weight of service.
     */
    void setWeight(Directory directory, int weight);

    /**
     * Removes the weight of service.
     */
    void removeWeight(Directory directory);

    /**
     * Warm-up time.
     */
    int getWarmUp();

    /**
     * Sets warm-up time.
     */
    void setWarmUp(int warmUp);

    /**
     * Returns {@code true} if warm up to complete.
     */
    boolean isWarmUpComplete();

    /**
     * Time of birth.
     */
    long timestamp();

    /**
     * Deadline millis.
     */
    long deadlineMillis();
}
