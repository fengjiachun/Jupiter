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

package org.jupiter.transport.error;

import org.jupiter.common.util.Signal;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.channel.JChannel;

import static org.jupiter.common.util.Signal.*;

/**
 * jupiter
 * org.jupiter.transport.error
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("all")
public class IoSignals {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(IoSignals.class);

    /** 错误的MAGIC */
    public static final Signal ILLEGAL_MAGIC    = valueOf(IoSignals.class, "ILLEGAL_MAGIC");
    /** 错误的消息标志位 */
    public static final Signal ILLEGAL_SIGN     = valueOf(IoSignals.class, "ILLEGAL_SIGN");
    /** Read idle 链路检测 */
    public static final Signal READER_IDLE      = valueOf(IoSignals.class, "READER_IDLE");
    /** Protocol body 太大 */
    public static final Signal BODY_TOO_LARGE   = valueOf(IoSignals.class, "BODY_TOO_LARGE");

    public static void handleSignal(Signal signal, JChannel channel) {
        logger.error("{} on {}, will force to close this channel.", signal.name(), channel);

        channel.close();
    }
}
