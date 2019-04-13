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
package org.jupiter.serialization.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jupiter.common.util.ThrowUtil;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.jupiter.serialization.io.InputBuf;
import org.jupiter.serialization.io.OutputBuf;
import org.jupiter.serialization.io.OutputStreams;
import org.jupiter.serialization.java.io.Inputs;
import org.jupiter.serialization.java.io.Outputs;

/**
 * Java自身的序列化/反序列化实现.
 *
 * jupiter
 * org.jupiter.serialization.java
 *
 * @author jiachun.fjc
 */
public class JavaSerializer extends Serializer {

    @Override
    public byte code() {
        return SerializerType.JAVA.value();
    }

    @Override
    public <T> OutputBuf writeObject(OutputBuf outputBuf, T obj) {
        try (ObjectOutputStream output = Outputs.getOutput(outputBuf)) {
            output.writeObject(obj);
            output.flush();
            return outputBuf;
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        }
        return null; // never get here
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        ByteArrayOutputStream buf = OutputStreams.getByteArrayOutputStream();
        try (ObjectOutputStream output = Outputs.getOutput(buf)) {
            output.writeObject(obj);
            output.flush();
            return buf.toByteArray();
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        } finally {
            OutputStreams.resetBuf(buf);
        }
        return null; // never get here
    }

    @Override
    public <T> T readObject(InputBuf inputBuf, Class<T> clazz) {
        try (ObjectInputStream input = Inputs.getInput(inputBuf)) {
            Object obj = input.readObject();
            return clazz.cast(obj);
        } catch (Exception e) {
            ThrowUtil.throwException(e);
        } finally {
            inputBuf.release();
        }
        return null; // never get here
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        try (ObjectInputStream input = Inputs.getInput(bytes, offset, length)) {
            Object obj = input.readObject();
            return clazz.cast(obj);
        } catch (Exception e) {
            ThrowUtil.throwException(e);
        }
        return null; // never get here
    }

    @Override
    public String toString() {
        return "java:(code=" + code() + ")";
    }
}
