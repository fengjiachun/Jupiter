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

import io.protostuff.ByteBufferInput;
import org.jupiter.serialization.InputBuf;

/**
 * jupiter
 * org.jupiter.serialization.proto.buffer
 *
 * @author jiachun.fjc
 */
public class InputFactory {

    public static ByteBufferInput getInput(InputBuf inputBuf) {
        return new ByteBufferInput(inputBuf.nioByteBuffer(), true);
    }

    private InputFactory() {}
}
