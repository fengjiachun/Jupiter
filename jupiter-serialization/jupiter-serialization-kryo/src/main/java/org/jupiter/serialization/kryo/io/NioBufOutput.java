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
package org.jupiter.serialization.kryo.io;

import org.jupiter.serialization.io.OutputBuf;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

/**
 * jupiter
 * org.jupiter.serialization.kryo.io
 *
 * @author jiachun.fjc
 */
class NioBufOutput extends ByteBufferOutput {

    protected final OutputBuf outputBuf;

    NioBufOutput(OutputBuf outputBuf, int minWritableBytes, int maxCapacity) {
        this.outputBuf = outputBuf;
        this.maxCapacity = maxCapacity;
        niobuffer = outputBuf.nioByteBuffer(minWritableBytes);
        capacity = niobuffer.remaining();
    }

    @Override
    protected boolean require(int required) throws KryoException {
        if (capacity - position >= required) {
            return false;
        }
        if (required > maxCapacity) {
            throw new KryoException("Buffer overflow. Max capacity: " + maxCapacity + ", required: " + required);
        }

        flush();

        while (capacity - position < required) {
            if (capacity == maxCapacity) {
                throw new KryoException("Buffer overflow. Available: " + (capacity - position) + ", required: " + required);
            }
            // Grow buffer.
            if (capacity == 0) {
                capacity = 1;
            }
            capacity = Math.min(capacity << 1, maxCapacity);
            if (capacity < 0) {
                capacity = maxCapacity;
            }
        }

        niobuffer = outputBuf.nioByteBuffer(capacity - position);
        capacity = niobuffer.limit();
        return true;
    }
}
