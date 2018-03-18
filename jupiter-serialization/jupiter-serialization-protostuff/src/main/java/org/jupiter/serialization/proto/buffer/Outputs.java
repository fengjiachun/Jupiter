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

package org.jupiter.serialization.proto.buffer;

import io.protostuff.LinkedBuffer;
import io.protostuff.Output;
import io.protostuff.ProtostuffOutput;
import org.jupiter.common.util.internal.InternalThreadLocal;
import org.jupiter.serialization.OutputBuf;

import static org.jupiter.serialization.Serializer.DEFAULT_BUF_SIZE;

/**
 * jupiter
 * org.jupiter.serialization.proto.buffer
 *
 * @author jiachun.fjc
 */
public class Outputs {

    // 复用 LinkedBuffer 中链表头结点 byte[]
    private static final InternalThreadLocal<LinkedBuffer> bufThreadLocal = new InternalThreadLocal<LinkedBuffer>() {

        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(DEFAULT_BUF_SIZE);
        }
    };

    public static Output getOutput(OutputBuf outputBuf) {
        if (outputBuf.hasMemoryAddress()) {
            return new UnsafeNioBufOutput(outputBuf, -1);
        }
        return new NioBufOutput(outputBuf, -1);
    }

    public static Output getOutput(LinkedBuffer buf) {
        return new ProtostuffOutput(buf);
    }

    public static LinkedBuffer getLinkedBuffer() {
        return bufThreadLocal.get();
    }

    public static byte[] toByteArray(Output output) {
        if (output instanceof ProtostuffOutput) {
            return ((ProtostuffOutput) output).toByteArray();
        }
        throw new UnsupportedOperationException();
    }

    private Outputs() {}
}
