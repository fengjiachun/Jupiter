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

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.InternalThreadLocal;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;

import java.util.concurrent.ConcurrentMap;

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

    private static final ConcurrentMap<Class<?>, Schema<?>> schemaCache = Maps.newConcurrentMap();

    // 目的是复用 LinkedBuffer 中链表头结点 byte[]
    private static final InternalThreadLocal<LinkedBuffer> bufThreadLocal = new InternalThreadLocal<LinkedBuffer>() {

        @Override
        protected LinkedBuffer initialValue() {
            return LinkedBuffer.allocate(DEFAULT_BUF_SIZE);
        }
    };

    @Override
    public byte code() {
        return SerializerType.PROTO_STUFF.value();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> byte[] writeObject(T obj) {
        Schema<T> schema = getSchema((Class<T>) obj.getClass());

        LinkedBuffer buf = bufThreadLocal.get();
        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buf);
        } finally {
            buf.clear(); // for reuse
        }
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        T msg = Reflects.newInstance(clazz, false);
        Schema<T> schema = getSchema(clazz);

        ProtostuffIOUtil.mergeFrom(bytes, offset, length, msg, schema);
        return msg;
    }

    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) schemaCache.get(clazz);
        if (schema == null) {
            Schema<T> newSchema = RuntimeSchema.createFrom(clazz);
            schema = (Schema<T>) schemaCache.putIfAbsent(clazz, newSchema);
            if (schema == null) {
                schema = newSchema;
            }
        }
        return schema;
    }

    @Override
    public String toString() {
        return "proto_stuff:(code=" + code() + ")";
    }
}
