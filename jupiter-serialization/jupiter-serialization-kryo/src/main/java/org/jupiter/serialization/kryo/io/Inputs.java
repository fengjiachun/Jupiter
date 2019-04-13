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

import java.nio.ByteBuffer;

import org.jupiter.serialization.io.InputBuf;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.FastInput;
import com.esotericsoftware.kryo.io.Input;

/**
 * jupiter
 * org.jupiter.serialization.kryo.io
 *
 * @author jiachun.fjc
 */
public final class Inputs {

    public static Input getInput(InputBuf inputBuf) {
        ByteBuffer nioBuf = inputBuf.nioByteBuffer();
        ByteBufferInput input = new ByteBufferInput();
        input.setVarIntsEnabled(false); // Compatible with FastInput
        input.setBuffer(nioBuf, 0, nioBuf.capacity());
        return input;
    }

    public static Input getInput(byte[] bytes, int offset, int length) {
        return new FastInput(bytes, offset, length);
    }

    private Inputs() {}
}
