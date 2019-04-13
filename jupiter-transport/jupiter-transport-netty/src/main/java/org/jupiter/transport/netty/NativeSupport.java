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
package org.jupiter.transport.netty;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;

/**
 * Netty provides the native socket transport using JNI.
 * This transport has higher performance and produces less garbage.
 *
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public final class NativeSupport {

    /**
     * The native socket transport for Linux using JNI.
     */
    public static boolean isNativeEPollAvailable() {
        return Epoll.isAvailable();
    }

    /**
     * The native socket transport for BSD systems such as MacOS using JNI.
     */
    public static boolean isNativeKQueueAvailable() {
        return KQueue.isAvailable();
    }
}
