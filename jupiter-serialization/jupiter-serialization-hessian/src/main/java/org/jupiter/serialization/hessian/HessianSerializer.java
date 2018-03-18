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

package org.jupiter.serialization.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import org.jupiter.common.util.ExceptionUtil;
import org.jupiter.serialization.InputBuf;
import org.jupiter.serialization.OutputBuf;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.jupiter.serialization.hessian.io.Inputs;
import org.jupiter.serialization.hessian.io.Outputs;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Hessian的序列化/反序列化实现
 *
 * jupiter
 * org.jupiter.serialization.hessian
 *
 * @author jiachun.fjc
 */
public class HessianSerializer extends Serializer {

    @Override
    public byte code() {
        return SerializerType.HESSIAN.value();
    }

    @Override
    public <T> OutputBuf writeObject(OutputBuf outputBuf, T obj) {
        Hessian2Output output = Outputs.getOutput(outputBuf);
        try {
            output.writeObject(obj);
            output.flush();
            return outputBuf;
        } catch (IOException e) {
            ExceptionUtil.throwException(e);
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {}
        }
        return null; // never get here
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        OutputStream buf = Outputs.getOutputStream();
        Hessian2Output output = Outputs.getOutput(buf);
        try {
            output.writeObject(obj);
            output.flush();
            return Outputs.toByteArray(buf);
        } catch (IOException e) {
            ExceptionUtil.throwException(e);
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {}

            Outputs.resetBuf(buf);
        }
        return null; // never get here
    }

    @Override
    public <T> T readObject(InputBuf inputBuf, Class<T> clazz) {
        Hessian2Input input = Inputs.getInput(inputBuf);
        try {
            return clazz.cast(input.readObject(clazz));
        } catch (IOException e) {
            ExceptionUtil.throwException(e);
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {}

            inputBuf.release();
        }
        return null; // never get here
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        Hessian2Input input = Inputs.getInput(bytes, offset, length);
        try {
            return clazz.cast(input.readObject(clazz));
        } catch (IOException e) {
            ExceptionUtil.throwException(e);
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {}
        }
        return null; // never get here
    }

    @Override
    public String toString() {
        return "hessian:(code=" + code() + ")";
    }
}
