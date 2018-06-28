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

import java.io.File;

/**
 * Unresolved address.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class UnresolvedDomainAddress implements UnresolvedAddress {

    private final String socketPath;

    public UnresolvedDomainAddress(String socketPath) {
        if (socketPath == null) {
            throw new NullPointerException("socketPath");
        }
        this.socketPath = socketPath;
    }

    public UnresolvedDomainAddress(File file) {
        this(file.getPath());
    }

    @Override
    public String getHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
        return socketPath;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof UnresolvedDomainAddress && ((UnresolvedDomainAddress) o).socketPath.equals(socketPath);

    }

    @Override
    public int hashCode() {
        return socketPath.hashCode();
    }

    @Override
    public String toString() {
        return getPath();
    }
}
