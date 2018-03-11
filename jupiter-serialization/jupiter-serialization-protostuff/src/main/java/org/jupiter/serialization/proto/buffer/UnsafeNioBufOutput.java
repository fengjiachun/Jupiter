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

package org.jupiter.serialization.proto.buffer;

import org.jupiter.serialization.OutputBuf;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;

import static org.jupiter.common.util.internal.UnsafeByteBufferUtil.*;

/**
 * jupiter
 * org.jupiter.serialization.proto.buffer
 *
 * @author jiachun.fjc
 */
class UnsafeNioBufOutput extends NioBufOutput {

    /**
     * Start address of the memory buffer The memory buffer should be non-movable, which normally means that is is allocated
     * off-heap
     */
    private long memoryAddress;

    UnsafeNioBufOutput(OutputBuf outputBuf, int minWritableBytes) {
        super(outputBuf, minWritableBytes);
        updateBufferAddress();
    }

    @Override
    protected void writeVarInt32(int value) throws IOException {
        byte[] buf = new byte[5];
        int locPtr = 0;
        int position = nioBuffer.position();
        while (true) {
            if ((value & ~0x7F) == 0) {
                buf[locPtr++] = (byte) value;
                ensureCapacity(locPtr);
                setBytes(address(position), buf, 0, locPtr);
                nioBuffer.position(position + locPtr);
                return;
            } else {
                buf[locPtr++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    @Override
    protected void writeVarInt64(long value) throws IOException {
        byte[] buf = new byte[10];
        int locPtr = 0;
        int position = nioBuffer.position();
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buf[locPtr++] = (byte) value;
                ensureCapacity(locPtr);
                setBytes(address(position), buf, 0, locPtr);
                nioBuffer.position(position + locPtr);
                return;
            } else {
                buf[locPtr++] = (byte) (((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    @Override
    protected void writeInt32LE(int value) throws IOException {
        ensureCapacity(4);
        int position = nioBuffer.position();
        setIntLE(address(position), value);
        nioBuffer.position(position + 4);
    }

    @Override
    protected void writeInt64LE(long value) throws IOException {
        ensureCapacity(8);
        int position = nioBuffer.position();
        setLongLE(address(position), value);
        nioBuffer.position(position + 8);
    }

    @Override
    protected void writeByte(byte value) throws IOException {
        ensureCapacity(1);
        int position = nioBuffer.position();
        setByte(address(position), value);
        nioBuffer.position(position + 1);
    }

    @Override
    protected void writeByteArray(byte[] value, int offset, int length) throws IOException {
        ensureCapacity(length);
        int position = nioBuffer.position();
        setBytes(address(position), value, offset, length);
        nioBuffer.position(position + length);
    }

    @Override
    protected void ensureCapacity(int required) {
        if (nioBuffer.remaining() < required) {
            int position = nioBuffer.position();

            while (capacity - position < required) {
                capacity = capacity << 1;
                if (capacity < 0) {
                    capacity = Integer.MAX_VALUE;
                }
            }

            nioBuffer = outputBuf.nioByteBuffer(capacity - position);
            capacity = nioBuffer.limit();
            // need to update the direct buffer's memory address
            updateBufferAddress();
        }
    }

    private void updateBufferAddress() {
        memoryAddress = ((DirectBuffer) nioBuffer).address();
    }

    private long address(int position) {
        return memoryAddress + position;
    }
}
