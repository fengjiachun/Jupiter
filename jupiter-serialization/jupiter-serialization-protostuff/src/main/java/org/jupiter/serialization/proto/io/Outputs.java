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
package org.jupiter.serialization.proto.io;

import io.protostuff.LinkedBuffer;
import io.protostuff.Output;
import io.protostuff.ProtostuffOutput;
import io.protostuff.WriteSession;

import org.jupiter.common.util.Reflects;
import org.jupiter.serialization.io.OutputBuf;

/**
 * jupiter
 * org.jupiter.serialization.proto.io
 *
 * @author jiachun.fjc
 */
public final class Outputs {

    public static Output getOutput(OutputBuf outputBuf) {
        if (outputBuf.hasMemoryAddress()) {
            return new UnsafeNioBufOutput(outputBuf, -1, Integer.MAX_VALUE);
        }
        return new NioBufOutput(outputBuf, -1, Integer.MAX_VALUE);
    }

    public static Output getOutput(LinkedBuffer buf) {
        return new ProtostuffOutput(buf);
    }

    public static byte[] toByteArray(Output output) {
        if (output instanceof WriteSession) {
            return ((WriteSession) output).toByteArray();
        }
        throw new IllegalArgumentException("Output [" + Reflects.simpleClassName(output)
                + "] must be a WriteSession's implementation");
    }

    private Outputs() {}
}
