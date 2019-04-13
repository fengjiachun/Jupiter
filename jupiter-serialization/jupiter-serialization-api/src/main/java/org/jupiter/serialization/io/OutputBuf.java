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
package org.jupiter.serialization.io;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * jupiter
 * org.jupiter.serialization.io
 *
 * @author jiachun.fjc
 */
public interface OutputBuf {

    /**
     * Exposes this backing data as an {@link OutputStream}.
     */
    OutputStream outputStream();

    /**
     * Exposes this backing data as a NIO {@link ByteBuffer}.
     */
    ByteBuffer nioByteBuffer(int minWritableBytes);

    /**
     * Returns the number of readable bytes.
     */
    int size();

    /**
     * Returns {@code true} if and only if this buf has a reference to the low-level memory address that points
     * to the backing data.
     */
    boolean hasMemoryAddress();

    /**
     * Returns the backing object.
     */
    Object backingObject();
}
