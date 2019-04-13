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

import java.io.ByteArrayOutputStream;

import org.jupiter.common.util.internal.InternalThreadLocal;
import org.jupiter.common.util.internal.UnsafeReferenceFieldUpdater;
import org.jupiter.common.util.internal.UnsafeUpdater;

import static org.jupiter.serialization.Serializer.DEFAULT_BUF_SIZE;
import static org.jupiter.serialization.Serializer.MAX_CACHED_BUF_SIZE;

/**
 * jupiter
 * org.jupiter.serialization.io
 *
 * @author jiachun.fjc
 */
public final class OutputStreams {

    private static final UnsafeReferenceFieldUpdater<ByteArrayOutputStream, byte[]> bufUpdater =
            UnsafeUpdater.newReferenceFieldUpdater(ByteArrayOutputStream.class, "buf");

    // 复用 ByteArrayOutputStream 中的 byte[]
    private static final InternalThreadLocal<ByteArrayOutputStream> bufThreadLocal = new InternalThreadLocal<ByteArrayOutputStream>() {

        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(DEFAULT_BUF_SIZE);
        }
    };

    public static ByteArrayOutputStream getByteArrayOutputStream() {
        return bufThreadLocal.get();
    }

    public static void resetBuf(ByteArrayOutputStream buf) {
        buf.reset(); // for reuse

        // 防止hold过大的内存块一直不释放
        assert bufUpdater != null;
        if (bufUpdater.get(buf).length > MAX_CACHED_BUF_SIZE) {
            bufUpdater.set(buf, new byte[DEFAULT_BUF_SIZE]);
        }
    }

    private OutputStreams() {}
}
