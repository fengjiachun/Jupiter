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

import io.protostuff.ByteArrayInput;
import io.protostuff.Input;
import io.protostuff.ProtobufException;

import org.jupiter.serialization.io.InputBuf;

/**
 * jupiter
 * org.jupiter.serialization.proto.io
 *
 * @author jiachun.fjc
 */
public final class Inputs {

    public static Input getInput(InputBuf inputBuf) {
        if (inputBuf.hasMemoryAddress()) {
            return new UnsafeNioBufInput(inputBuf.nioByteBuffer(), true);
        }
        return new NioBufInput(inputBuf.nioByteBuffer(), true);
    }

    public static Input getInput(byte[] bytes, int offset, int length) {
        return new ByteArrayInput(bytes, offset, length, true);
    }

    public static void checkLastTagWas(Input input, final int value) throws ProtobufException {
        if (input instanceof UnsafeNioBufInput) {
            ((UnsafeNioBufInput) input).checkLastTagWas(value);
        } else if (input instanceof NioBufInput) {
            ((NioBufInput) input).checkLastTagWas(value);
        } else if (input instanceof ByteArrayInput) {
            ((ByteArrayInput) input).checkLastTagWas(value);
        }
    }

    private Inputs() {}
}
