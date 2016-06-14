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

package org.jupiter.rpc;

import org.jupiter.registry.NotifyListener;
import org.jupiter.registry.OfflineListener;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.Registry;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;

import java.util.Collection;

import static org.jupiter.rpc.channel.DirectoryJChannelGroup.CopyOnWriteGroupList;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JClient extends Registry {

    /**
     * Everyone should got a app name.
     */
    String appName();

    /**
     * Returns or new a {@link JChannelGroup}.
     */
    JChannelGroup group(UnresolvedAddress address);

    /**
     * Returns all {@link JChannelGroup}s.
     */
    Collection<JChannelGroup> groups();

    /**
     * Adds a {@link JChannelGroup} by {@link Directory}.
     */
    boolean addChannelGroup(Directory directory, JChannelGroup group);

    /**
     * Removes a {@link JChannelGroup} by {@link Directory}.
     */
    boolean removeChannelGroup(Directory directory, JChannelGroup group);

    /**
     * Returns list of {@link JChannelGroup}s by the same {@link Directory}.
     */
    CopyOnWriteGroupList directory(Directory directory);

    /**
     * Returns {@code true} if has available {@link JChannelGroup}s
     * on this {@link Directory}.
     */
    boolean isDirectoryAvailable(Directory directory);

    /**
     * Selects a {@link JChannel} from the load balancer.
     */
    JChannel select(Directory directory);

    /**
     * Find a service in the local scope.
     */
    Collection<RegisterMeta> lookup(Directory directory);

    /**
     * Wait until the connections is available or timeout,
     * if available return true, otherwise return false.
     */
    boolean awaitConnections(Directory directory, long timeoutMillis);

    /**
     * Refresh the connections.
     */
    void refreshConnections(Directory directory);

    /**
     * Subscribe a service from registry server.
     */
    void subscribe(Directory directory, NotifyListener listener);

    /**
     * Provider offline notification.
     */
    void offlineListening(UnresolvedAddress address, OfflineListener listener);
}
