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
package org.jupiter.rpc.exception;

import java.net.SocketAddress;

/**
 * Remote call abnormalities.
 *
 * jupiter
 * org.jupiter.rpc.exception
 *
 * @author jiachun.fjc
 */
public class JupiterRemoteException extends RuntimeException {

    private static final long serialVersionUID = -6516335527982400712L;

    private final SocketAddress remoteAddress;

    public JupiterRemoteException() {
        this.remoteAddress = null;
    }

    public JupiterRemoteException(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public JupiterRemoteException(Throwable cause) {
        super(cause);
        this.remoteAddress = null;
    }

    public JupiterRemoteException(Throwable cause, SocketAddress remoteAddress) {
        super(cause);
        this.remoteAddress = remoteAddress;
    }

    public JupiterRemoteException(String message) {
        super(message);
        this.remoteAddress = null;
    }

    public JupiterRemoteException(String message, SocketAddress remoteAddress) {
        super(message);
        this.remoteAddress = remoteAddress;
    }

    public JupiterRemoteException(String message, Throwable cause) {
        super(message, cause);
        this.remoteAddress = null;
    }

    public JupiterRemoteException(String message, Throwable cause, SocketAddress remoteAddress) {
        super(message, cause);
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}
