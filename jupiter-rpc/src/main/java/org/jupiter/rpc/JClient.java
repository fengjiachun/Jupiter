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
import org.jupiter.transport.Directory;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.UnresolvedAddress;

import java.util.Collection;

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
     * Returns the connector.
     */
    JConnector<JConnection> connector();

    /**
     * Sets the connector.
     */
    JClient connector(JConnector<JConnection> connector);

    /**
     * Find a service in the local scope.
     */
    Collection<RegisterMeta> lookup(Directory directory);

    /**
     * Sets auto manage the connections.
     */
    JConnector.ConnectionManager manageConnections(Class<?> interfaceClass);

    /**
     * Sets auto manage the connections.
     */
    JConnector.ConnectionManager manageConnections(Class<?> interfaceClass, String version);

    /**
     * Sets auto manage the connections.
     */
    JConnector.ConnectionManager manageConnections(Directory directory);

    /**
     * Wait until the connections is available or timeout,
     * if available return true, otherwise return false.
     */
    boolean awaitConnections(Directory directory, long timeoutMillis);

    /**
     * Subscribe a service from registry server.
     */
    void subscribe(Directory directory, NotifyListener listener);

    /**
     * Provider offline notification.
     */
    void offlineListening(UnresolvedAddress address, OfflineListener listener);

    /**
     * Shutdown.
     */
    void shutdownGracefully();
}
