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

import java.net.SocketAddress;
import java.util.EventListener;

/**
 * RPC callback, a service object all methods share a {@link JListener},
 * as the difference between a parameter request.
 *
 * Note:
 * If {@link JListener#complete(JRequest, JResult)} thrown a {@link Exception} during execution,
 * will trigger {@link JListener#failure(JRequest, Throwable)}
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JListener extends EventListener {

    /**
     * Returns result when the call succeeds.
     */
    void complete(JRequest request, JResult result) throws Exception;

    /**
     * Returns an exception message when call fails.
     */
    void failure(JRequest request, Throwable cause);

    class JResult {
        private final SocketAddress remoteAddress;
        private final Object value;

        public JResult(SocketAddress remoteAddress, Object value) {
            this.remoteAddress = remoteAddress;
            this.value = value;
        }

        public SocketAddress remoteAddress() {
            return remoteAddress;
        }

        public Object value() {
            return value;
        }

        @Override
        public String toString() {
            return "JResult{" +
                    "remoteAddress=" + remoteAddress +
                    ", value=" + value +
                    '}';
        }
    }
}
