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
package org.jupiter.serialization.kryo.io;

import org.jupiter.common.util.internal.InternalThreadLocal;
import org.jupiter.serialization.io.OutputBuf;

import com.esotericsoftware.kryo.io.FastOutput;
import com.esotericsoftware.kryo.io.Output;

import static org.jupiter.serialization.Serializer.DEFAULT_BUF_SIZE;
import static org.jupiter.serialization.Serializer.MAX_CACHED_BUF_SIZE;

/**
 * jupiter
 * org.jupiter.serialization.kryo.io
 *
 * @author jiachun.fjc
 */
public final class Outputs {

    // 复用 Output 中的 byte[]
    private static final InternalThreadLocal<Output> outputBytesThreadLocal = new InternalThreadLocal<Output>() {

        @Override
        protected Output initialValue() {
            return new FastOutput(DEFAULT_BUF_SIZE, -1);
        }
    };

    public static Output getOutput(OutputBuf outputBuf) {
        NioBufOutput output = new NioBufOutput(outputBuf, -1, Integer.MAX_VALUE);
        output.setVarIntsEnabled(false); // Compatible with FastOutput
        return output;
    }

    public static Output getOutput() {
        return outputBytesThreadLocal.get();
    }

    public static void clearOutput(Output output) {
        output.clear();

        // 防止hold过大的内存块一直不释放
        byte[] bytes = output.getBuffer();
        if (bytes == null) {
            return;
        }
        if (bytes.length > MAX_CACHED_BUF_SIZE) {
            output.setBuffer(new byte[DEFAULT_BUF_SIZE], -1);
        }
    }

    private Outputs() {}
}
