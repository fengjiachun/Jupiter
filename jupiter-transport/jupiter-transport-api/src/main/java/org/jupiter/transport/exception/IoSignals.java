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

    /** 错误的MAGIC */
    public static final Signal ILLEGAL_MAGIC    = Signal.valueOf(IoSignals.class, "ILLEGAL_MAGIC");
    /** 错误的消息标志位 */
    public static final Signal ILLEGAL_SIGN     = Signal.valueOf(IoSignals.class, "ILLEGAL_SIGN");
    /** Read idle 链路检测 */
    public static final Signal READER_IDLE      = Signal.valueOf(IoSignals.class, "READER_IDLE");
    /** Protocol body 太大 */
    public static final Signal BODY_TOO_LARGE   = Signal.valueOf(IoSignals.class, "BODY_TOO_LARGE");
}
