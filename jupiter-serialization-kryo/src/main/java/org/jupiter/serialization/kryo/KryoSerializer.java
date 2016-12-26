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
import org.jupiter.common.util.internal.InternalThreadLocal;
import org.jupiter.common.util.internal.UnsafeReferenceFieldUpdater;
import org.jupiter.common.util.internal.UnsafeUpdater;
import org.jupiter.serialization.Serializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import static org.jupiter.serialization.SerializerType.KRYO;

/**
 * jupiter
 * org.jupiter.serialization.kryo
 *
 * @author jiachun.fjc
 */
public class KryoSerializer extends Serializer {

    private static final UnsafeReferenceFieldUpdater<Output, byte[]> bufUpdater =
            UnsafeUpdater.newReferenceFieldUpdater(Output.class, "buffer");

    private static final int DISCARD_LIMIT = 1024 << 4; // 16k

    private static final InternalThreadLocal<Kryo> kryoThreadLocal = new InternalThreadLocal<Kryo>() {

        @Override
        protected Kryo initialValue() throws Exception {
            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            kryo.setRegistrationRequired(false);
            kryo.setReferences(false);
            return kryo;
        }
    };

    private static final InternalThreadLocal<Output> outputThreadLocal = new InternalThreadLocal<Output>() {

        @Override
        protected Output initialValue() {
            return new Output(512);
        }
    };

    @Override
    public byte code() {
        return KRYO.value();
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

            // 防止hold太大块的内存
            if (bufUpdater.get(output).length > DISCARD_LIMIT) {
                bufUpdater.set(output, new byte[512]);
            }
        }
    }

    @Override
    public <T> T readObject(byte[] bytes, Class<T> clazz) {
        return readObject(bytes, 0, bytes.length, clazz);
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
