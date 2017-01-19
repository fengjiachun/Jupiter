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

package org.jupiter.transport.exception;

import org.jupiter.common.util.Signal;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.channel.JChannel;

import static org.jupiter.common.util.Signal.*;

/**
 * {@link Signal} has an empty stack trace, you can throw them just like using goto.
 *
 * 当全局goto用的, {@link Signal}有一个空堆栈，你可以像使用goto一样抛出它们.
 *
 * jupiter
 * org.jupiter.transport.exception
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
