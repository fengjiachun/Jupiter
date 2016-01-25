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

import org.jupiter.rpc.Status;

import java.net.SocketAddress;

/**
 * Call timeout, usually thrown by client.
 *
 * jupiter
 * org.jupiter.rpc.exception
 *
 * @author jiachun.fjc
 */
public class TimeoutException extends RemoteException {

    private static final long serialVersionUID = 8768621104391094458L;

    private final Status status;

    public TimeoutException(SocketAddress remoteAddress, Status status) {
        super(remoteAddress);
        this.status = status;
    }

    public TimeoutException(Throwable cause, SocketAddress remoteAddress, Status status) {
        super(cause, remoteAddress);
        this.status = status;
    }

    public TimeoutException(String message, SocketAddress remoteAddress, Status status) {
        super(message, remoteAddress);
        this.status = status;
    }

    public TimeoutException(String message, Throwable cause, SocketAddress remoteAddress, Status status) {
        super(message, cause, remoteAddress);
        this.status = status;
    }

    public Status status() {
        return status;
    }

    @Override
    public String toString() {
        return "TimeoutException{" +
                "status=" + status +
                '}';
    }
}
