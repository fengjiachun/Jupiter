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

package org.jupiter.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import org.jupiter.common.concurrent.collection.ConcurrentSet;
import org.jupiter.common.util.internal.InternalThreadLocal;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * jupiter
 * org.jupiter.serialization.kryo
 *
 * @author jiachun.fjc
 */
public class KryoSerializer extends Serializer {

    private static ConcurrentSet<Class<?>> useJavaSerializerTypes = new ConcurrentSet<>();

    static {
        useJavaSerializerTypes.add(Throwable.class);
    }

    private static final InternalThreadLocal<Kryo> kryoThreadLocal = new InternalThreadLocal<Kryo>() {

        @Override
        protected Kryo initialValue() throws Exception {
            Kryo kryo = new Kryo();
            for (Class<?> type : useJavaSerializerTypes) {
                kryo.addDefaultSerializer(type, JavaSerializer.class);
            }
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            kryo.setRegistrationRequired(false);
            kryo.setReferences(false);
            return kryo;
        }
    };

    private static final InternalThreadLocal<Output> outputThreadLocal = new InternalThreadLocal<Output>() {

        @Override
        protected Output initialValue() {
            return new Output(DEFAULT_BUF_SIZE, -1);
        }
    };

    /**
     * Serializes {@code type}'s objects using Java's built in serialization mechanism,
     * note that this is very inefficient and should be avoided if possible.
     */
    public static void setJavaSerializer(Class<?> type) {
        useJavaSerializerTypes.add(type);
    }

    @Override
    public byte code() {
        return SerializerType.KRYO.value();
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        Output output = outputThreadLocal.get();
        try {
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(output, obj);
            return output.toBytes();
        } finally {
            output.clear();

            // 防止hold过大的内存块一直不释放
            if (output.getBuffer().length > MAX_CACHED_BUF_SIZE) {
                output.setBuffer(new byte[DEFAULT_BUF_SIZE], -1);
            }
        }
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        Input input = new Input(bytes, offset, length);
        Kryo kryo = kryoThreadLocal.get();
        return kryo.readObject(input, clazz);
    }

    @Override
    public String toString() {
        return "kryo:(code=" + code() + ")";
    }
}
