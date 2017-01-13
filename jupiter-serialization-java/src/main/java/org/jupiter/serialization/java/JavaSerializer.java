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

import org.jupiter.common.util.internal.InternalThreadLocal;
import org.jupiter.common.util.internal.JUnsafe;
import org.jupiter.common.util.internal.UnsafeReferenceFieldUpdater;
import org.jupiter.common.util.internal.UnsafeUpdater;
import org.jupiter.serialization.Serializer;

import java.io.*;

import static org.jupiter.serialization.SerializerType.JAVA;

/**
 * jupiter
 * org.jupiter.serialization.java
 *
 * @author jiachun.fjc
 */
public class JavaSerializer extends Serializer {

    private static final UnsafeReferenceFieldUpdater<ByteArrayOutputStream, byte[]> bufUpdater =
            UnsafeUpdater.newReferenceFieldUpdater(ByteArrayOutputStream.class, "buf");

    private static final InternalThreadLocal<ByteArrayOutputStream> bufThreadLocal = new InternalThreadLocal<ByteArrayOutputStream>() {

        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(DEFAULT_BUF_SIZE);
        }
    };

    @Override
    public byte code() {
        return JAVA.value();
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        ByteArrayOutputStream buf = bufThreadLocal.get();
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(buf);
            output.writeObject(obj);
            return buf.toByteArray();
        } catch (IOException e) {
            JUnsafe.throwException(e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {}
            }

            buf.reset(); // for reuse

            // 防止hold太大块的内存
            if (bufUpdater.get(buf).length > MAX_CACHED_BUF_SIZE) {
                bufUpdater.set(buf, new byte[DEFAULT_BUF_SIZE]);
            }
        }
        return null; // never get here
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new ByteArrayInputStream(bytes, offset, length));
            Object obj = input.readObject();
            return clazz.cast(obj);
        } catch (Exception e) {
            JUnsafe.throwException(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {}
            }
        }
        return null; // never get here
    }

    @Override
    public String toString() {
        return "java:(code=" + code() + ")";
    }
}
