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

import org.jupiter.common.util.SystemPropertyUtil;

/**
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class LowCopy {

    private static final boolean DECODE_LOW_COPY =
            SystemPropertyUtil.getBoolean("jupiter.transport.decode.low_copy", true);
    private static final boolean ENCODE_LOW_COPY =
            SystemPropertyUtil.getBoolean("jupiter.transport.encode.low_copy", false);

    public static boolean isDecodeLowCopy() {
        return DECODE_LOW_COPY;
    }

    public static boolean isEncodeLowCopy() {
        return ENCODE_LOW_COPY;
    }
}
