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

import org.jupiter.rpc.Directory;
import org.jupiter.rpc.UnresolvedAddress;

/**
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JConnector<C> extends Transporter {

    /**
     * Connect to the remote peer.
     */
    C connect(UnresolvedAddress remoteAddress);

    /**
     * Connect to the remote peer.
     */
    C connect(UnresolvedAddress remoteAddress, boolean async);

    /**
     * Sets auto manage the connections
     */
    ConnectionManager manageConnections(Directory directory);

    /**
     * Server options [parent, child]
     */
    JConfig config();

    /**
     * Shutdown the server
     */
    void shutdownGracefully();

    interface ConnectionManager {

        void start();

        /**
         * Wait until the connections is available or timeout,
         * if available return true, otherwise return false.
         */
        boolean waitForAvailable(long timeoutMillis);
    }
}
