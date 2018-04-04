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

/**
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public abstract class JConnection {

    private final UnresolvedAddress address;

    public JConnection(UnresolvedAddress address) {
        this.address = address;
    }

    public UnresolvedAddress getAddress() {
        return address;
    }

    public void operationComplete(@SuppressWarnings("unused") OperationListener operationListener) {
        // the default implementation does nothing
    }

    public abstract void setReconnect(boolean reconnect);

    public interface OperationListener {

        void complete(boolean isSuccess);
    }
}
