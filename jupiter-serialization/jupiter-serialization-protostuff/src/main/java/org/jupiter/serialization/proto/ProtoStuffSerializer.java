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

package org.jupiter.serialization.proto;

import io.protostuff.Input;
import io.protostuff.LinkedBuffer;
import io.protostuff.Output;
import io.protostuff.Schema;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;
import org.jupiter.common.util.ClassUtil;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.ThrowUtil;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.jupiter.serialization.io.InputBuf;
import org.jupiter.serialization.io.OutputBuf;
import org.jupiter.serialization.proto.io.Inputs;
import org.jupiter.serialization.proto.io.LinkedBuffers;
import org.jupiter.serialization.proto.io.Outputs;

import java.io.IOException;

/**
 * Protostuff的序列化/反序列化实现, jupiter中默认的实现.
 *
 * jupiter
 * org.jupiter.serialization.proto
 *
 * @author jiachun.fjc
 */
public class ProtoStuffSerializer extends Serializer {

    static {
        ClassUtil.forClass(IdStrategy.class);

        // 详见 io.protostuff.runtime.RuntimeEnv

        // If true, the constructor will always be obtained from {@code ReflectionFactory.newConstructorFromSerialization}.
        //
        // Enable this if you intend to avoid deserialize objects whose no-args constructor initializes (unwanted)
        // internal state. This applies to complex/framework objects.
        //
        // If you intend to fill default field values using your default constructor, leave this disabled. This normally
        // applies to java beans/data objects.
        //
        // 默认 true, 禁止反序列化时构造方法被调用, 防止有些类的构造方法内有令人惊喜的逻辑
        String always_use_sun_reflection_factory = SystemPropertyUtil
                .get("jupiter.serializer.protostuff.always_use_sun_reflection_factory", "true");
        SystemPropertyUtil
                .setProperty("protostuff.runtime.always_use_sun_reflection_factory", always_use_sun_reflection_factory);

        // Disabled by default.  Writes a sentinel value (uint32) in place of null values.
        //
        // 默认 false, 不允许数组中的元素为 null
        String allow_null_array_element = SystemPropertyUtil
                .get("jupiter.serializer.protostuff.allow_null_array_element", "false");
        SystemPropertyUtil
                .setProperty("protostuff.runtime.allow_null_array_element", allow_null_array_element);
    }

    @Override
    public byte code() {
        return SerializerType.PROTO_STUFF.value();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> OutputBuf writeObject(OutputBuf outputBuf, T obj) {
        Schema<T> schema = RuntimeSchema.getSchema((Class<T>) obj.getClass());

        Output output = Outputs.getOutput(outputBuf);
        try {
            schema.writeTo(output, obj);
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        }

        return outputBuf;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> byte[] writeObject(T obj) {
        Schema<T> schema = RuntimeSchema.getSchema((Class<T>) obj.getClass());

        LinkedBuffer buf = LinkedBuffers.getLinkedBuffer();
        Output output = Outputs.getOutput(buf);
        try {
            schema.writeTo(output, obj);
            return Outputs.toByteArray(output);
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        } finally {
            LinkedBuffers.resetBuf(buf); // for reuse
        }

        return null; // never get here
    }

    @Override
    public <T> T readObject(InputBuf inputBuf, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T msg = schema.newMessage();

        Input input = Inputs.getInput(inputBuf);
        try {
            schema.mergeFrom(input, msg);
            Inputs.checkLastTagWas(input, 0);
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        } finally {
            inputBuf.release();
        }

        return msg;
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T msg = schema.newMessage();

        Input input = Inputs.getInput(bytes, offset, length);
        try {
            schema.mergeFrom(input, msg);
            Inputs.checkLastTagWas(input, 0);
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        }

        return msg;
    }

    @Override
    public String toString() {
        return "proto_stuff:(code=" + code() + ")";
    }
}
