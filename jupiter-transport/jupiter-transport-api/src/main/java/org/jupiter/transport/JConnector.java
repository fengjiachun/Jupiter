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
package org.jupiter.transport;

import java.util.Collection;

import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.DirectoryJChannelGroup;
import org.jupiter.transport.channel.JChannelGroup;
import org.jupiter.transport.processor.ConsumerProcessor;

/**
 * 注意 JConnector 单例即可, 不要创建多个实例.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JConnector<C> extends Transporter {

    /**
     * Connector options [parent, child].
     */
    JConfig config();

    /**
     * Returns the rpc processor.
     */
    ConsumerProcessor processor();

    /**
     * Binds the rpc processor.
     */
    void withProcessor(ConsumerProcessor processor);

    /**
     * Connects to the remote peer.
     */
    C connect(UnresolvedAddress address);

    /**
     * Connects to the remote peer.
     */
    C connect(UnresolvedAddress address, boolean async);

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
     * Returns the {@link DirectoryJChannelGroup}.
     */
    DirectoryJChannelGroup directoryGroup();

    /**
     * Returns the {@link JConnectionManager}.
     */
    JConnectionManager connectionManager();

    /**
     * Shutdown the server.
     */
    void shutdownGracefully();

    interface ConnectionWatcher {

        /**
         * Start to connect to server.
         */
        void start();

        /**
         * Wait until the connections is available or timeout,
         * if available return true, otherwise return false.
         */
        boolean waitForAvailable(long timeoutMillis);
    }
}
