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

import org.jupiter.rpc.JServer;

import java.net.SocketAddress;

/**
 * Server acceptor.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JAcceptor extends JServer, Transporter {

    /**
     * Local address.
     */
    SocketAddress localAddress();

    /**
     * Server options [parent, child].
     */
    JConfigGroup configGroup();

    /**
     * Start the server and wait until the server socket is closed.
     */
    void start() throws InterruptedException;

    /**
     * Start the server.
     */
    void start(boolean sync) throws InterruptedException;

    /**
     * Shutdown the server gracefully.
     */
    void shutdownGracefully();
}
