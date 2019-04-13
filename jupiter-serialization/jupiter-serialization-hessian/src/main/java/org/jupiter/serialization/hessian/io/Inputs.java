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
package org.jupiter.serialization.hessian.io;

import java.io.ByteArrayInputStream;

import org.jupiter.serialization.io.InputBuf;

import com.caucho.hessian.io.Hessian2Input;

/**
 * jupiter
 * org.jupiter.serialization.hessian.io
 *
 * @author jiachun.fjc
 */
public final class Inputs {

    public static Hessian2Input getInput(InputBuf inputBuf) {
        return new Hessian2Input(inputBuf.inputStream());
    }

    public static Hessian2Input getInput(byte[] bytes, int offset, int length) {
        return new Hessian2Input(new ByteArrayInputStream(bytes, offset, length));
    }

    private Inputs() {}
}
