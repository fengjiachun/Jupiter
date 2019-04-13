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
package org.jupiter.serialization;

import org.jupiter.serialization.io.InputBuf;
import org.jupiter.serialization.io.OutputBuf;

/**
 * This interface provides an abstract view for one or more serializer impl.
 *
 * SerializerImpl是基于SPI加载的, 会加载所有(jupiter-serialization-XXX), 并可以同时可以支持所有引入的SerializerImpl.
 *
 * jupiter
 * org.jupiter.serialization
 *
 * @author jiachun.fjc
 */
public abstract class Serializer {

    /**
     * The max buffer size for a {@link Serializer} to cached.
     */
    public static final int MAX_CACHED_BUF_SIZE = 256 * 1024;

    /**
     * The default buffer size for a {@link Serializer}.
     */
    public static final int DEFAULT_BUF_SIZE = 512;

    public abstract byte code();

    public abstract <T> OutputBuf writeObject(OutputBuf outputBuf, T obj);

    public abstract <T> byte[] writeObject(T obj);

    public abstract <T> T readObject(InputBuf inputBuf, Class<T> clazz);

    public abstract <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz);

    public <T> T readObject(byte[] bytes, Class<T> clazz) {
        return readObject(bytes, 0, bytes.length, clazz);
    }
}
