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

package org.jupiter.serialization.kryo.buffer;

import com.esotericsoftware.kryo.io.Output;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.serialization.OutputBuf;

/**
 * jupiter
 * org.jupiter.serialization.kryo.buffer
 *
 * @author jiachun.fjc
 */
public final class OutputFactory {

    private static final boolean USE_UNSAFE_OUTPUT =
            SystemPropertyUtil.getBoolean("jupiter.serialization.kryo.use_unsafe_output", false);

    public static Output getOutput(OutputBuf outputBuf) {
        if (USE_UNSAFE_OUTPUT && outputBuf.hasMemoryAddress()) {
            return new UnsafeNioBufOutput(outputBuf, -1);
        } else {
            return new NioBufOutput(outputBuf, -1);
        }
    }

    private OutputFactory() {}
}
