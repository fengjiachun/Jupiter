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

import org.jupiter.common.util.internal.JUnsafe;
import org.jupiter.serialization.OutputBuf;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * jupiter
 * org.jupiter.serialization.proto.buffer
 *
 * @author jiachun.fjc
 */
class UnsafeNioBufOutput extends NioBufOutput {

    private static final boolean UNALIGNED = JUnsafe.isUnaligned();
    private static final boolean BIG_ENDIAN_NATIVE_ORDER = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    private static final Unsafe unsafe = JUnsafe.getUnsafe();

    /**
     * Start address of the memory buffer The memory buffer should be non-movable, which normally means that is is allocated
     * off-heap
     */
    private long memoryAddress;
    private int position;

    UnsafeNioBufOutput(OutputBuf outputBuf, int minWritableBytes) {
        super(outputBuf, minWritableBytes);
        position = nioBuffer.position();
        updateBufferAddress();
    }

    @Override
    protected void writeVarInt32(int value) throws IOException {
        position = nioBuffer.position();
        if (value >>> 7 == 0) {
            ensureCapacity(1);
            setByte(address(position++), value);
            nioBuffer.position(position);
            return;
        }

        if (value >>> 14 == 0) {
            ensureCapacity(2);
            int newValue = (((value & 0x7F) | 0x80) << 8) | (value >>> 7);
            setShort(address(position), newValue);
            position += 2;
            nioBuffer.position(position);
            return;
        }

        if (value >>> 21 == 0) {
            ensureCapacity(3);
            int newValue = (((value & 0x7F) | 0x80) << 8) | (value >>> 7 & 0xFF | 0x80);
            setShort(address(position), newValue);
            position += 2;
            setByte(address(position++), value >>> 14);
            nioBuffer.position(position);
            return;
        }

        if (value >>> 28 == 0) {
            ensureCapacity(4);
            int newValue = (((value & 0x7F) | 0x80) << 24)
                    | ((value >>> 7 & 0xFF | 0x80) << 16)
                    | ((value >>> 14 & 0xFF | 0x80) << 8)
                    | (value >>> 21);
            setInt(address(position), newValue);
            position += 4;
            nioBuffer.position(position);
            return;
        }

        ensureCapacity(5);
        int newValue = (((value & 0x7F) | 0x80) << 24)
                | ((value >>> 7 & 0xFF | 0x80) << 16)
                | ((value >>> 14 & 0xFF | 0x80) << 8)
                | (value >>> 21 & 0xFF | 0x80);
        setInt(address(position), newValue);
        position += 4;
        setByte(address(position++), value >>> 28);
        nioBuffer.position(position);
    }

    @Override
    protected void writeVarInt64(long value) throws IOException {
        position = nioBuffer.position();
        if (value >>> 7 == 0) {
            ensureCapacity(1);
            setByte(address(position++), (int) value);
            nioBuffer.position(position);
            return;
        }

        if (value >>> 14 == 0) {
            ensureCapacity(2);
            int intValue = (int) value;
            int newValue = (((intValue & 0x7F) | 0x80) << 8) | (intValue >>> 7);
            setShort(address(position), newValue);
            position += 2;
            nioBuffer.position(position);
            return;
        }

        if (value >>> 21 == 0) {
            ensureCapacity(3);
            int intValue = (int) value;
            int newValue = (((intValue & 0x7F) | 0x80) << 8) | (intValue >>> 7 & 0xFF | 0x80);
            setShort(address(position), newValue);
            position += 2;
            setByte(address(position++), intValue >>> 14);
            nioBuffer.position(position);
            return;
        }

        if (value >>> 28 == 0) {
            ensureCapacity(4);
            int intValue = (int) value;
            int newValue = (((intValue & 0x7F) | 0x80) << 24)
                    | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                    | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                    | (intValue >>> 21);
            setInt(address(position), newValue);
            position += 4;
            nioBuffer.position(position);
            return;
        }

        if (value >>> 35 == 0) {
            ensureCapacity(5);
            int intValue = (int) value;
            int newValue = (((intValue & 0x7F) | 0x80) << 24)
                    | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                    | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                    | (intValue >>> 21 & 0xFF | 0x80);
            setInt(address(position), newValue);
            position += 4;
            setByte(address(position++), (int) (value >>> 28));
            nioBuffer.position(position);
            return;
        }

        if (value >>> 42 == 0) {
            ensureCapacity(6);
            int intValue = (int) value;
            int first = (((intValue & 0x7F) | 0x80) << 24)
                    | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                    | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                    | (intValue >>> 21 & 0xFF | 0x80);
            int second = (int) (((value >>> 28 & 0xFF | 0x80) << 8)
                    | (value >>> 35));
            setInt(address(position), first);
            position += 4;
            setShort(address(position), second);
            position += 2;
            nioBuffer.position(position);
            return;
        }

        if (value >>> 49 == 0) {
            ensureCapacity(7);
            int intValue = (int) value;
            int first = (((intValue & 0x7F) | 0x80) << 24)
                    | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                    | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                    | (intValue >>> 21 & 0xFF | 0x80);
            int second = (int) (((value >>> 28 & 0xFF | 0x80) << 8)
                    | (value >>> 35 & 0xFF | 0x80));
            setInt(address(position), first);
            position += 4;
            setShort(address(position), second);
            position += 2;
            setByte(address(position++), (int) (value >>> 42));
            nioBuffer.position(position);
            return;
        }

        if (value >>> 56 == 0) {
            ensureCapacity(8);
            int intValue = (int) value;
            int first = (((intValue & 0x7F) | 0x80) << 24)
                    | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                    | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                    | (intValue >>> 21 & 0xFF | 0x80);
            intValue = (int) (value >>> 28);
            int second = (((intValue & 0x7F) | 0x80) << 24)
                    | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                    | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                    | (intValue >>> 21);
            setInt(address(position), first);
            position += 4;
            setInt(address(position), second);
            position += 4;
            nioBuffer.position(position);
            return;
        }

        ensureCapacity(9);
        int intValue = (int) value;
        int first = (((intValue & 0x7F) | 0x80) << 24)
                | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                | (intValue >>> 21 & 0xFF | 0x80);
        intValue = (int) (value >>> 28);
        int second = (((intValue & 0x7F) | 0x80) << 24)
                | ((intValue >>> 7 & 0xFF | 0x80) << 16)
                | ((intValue >>> 14 & 0xFF | 0x80) << 8)
                | (intValue >>> 21 & 0xFF | 0x80);
        setInt(address(position), first);
        position += 4;
        setInt(address(position), second);
        position += 4;
        setByte(address(position++), (int) (value >>> 56));
        nioBuffer.position(position);
    }

    @Override
    protected void writeInt32LE(int value) throws IOException {
        position = nioBuffer.position();
        ensureCapacity(4);
        setIntLE(address(position), value);
        position += 4;
        nioBuffer.position(position);
    }

    @Override
    protected void writeInt64LE(long value) throws IOException {
        position = nioBuffer.position();
        ensureCapacity(8);
        setLongLE(address(position), value);
        position += 8;
        nioBuffer.position(position);
    }

    @Override
    protected void writeByte(byte value) throws IOException {
        position = nioBuffer.position();
        ensureCapacity(1);
        setByte(address(position++), value);
        nioBuffer.position(position);
    }

    static void setByte(long address, int value) {
        unsafe.putByte(address, (byte) value);
    }

    static void setShort(long address, int value) {
        if (UNALIGNED) {
            unsafe.putShort(address, BIG_ENDIAN_NATIVE_ORDER ? (short) value : Short.reverseBytes((short) value));
        } else {
            unsafe.putByte(address, (byte) (value >>> 8));
            unsafe.putByte(address + 1, (byte) value);
        }
    }

    static void setInt(long address, int value) {
        if (UNALIGNED) {
            unsafe.putInt(address, BIG_ENDIAN_NATIVE_ORDER ? value : Integer.reverseBytes(value));
        } else {
            unsafe.putByte(address, (byte) (value >>> 24));
            unsafe.putByte(address + 1, (byte) (value >>> 16));
            unsafe.putByte(address + 2, (byte) (value >>> 8));
            unsafe.putByte(address + 3, (byte) value);
        }
    }

    static void setIntLE(long address, int value) {
        if (UNALIGNED) {
            unsafe.putInt(address, BIG_ENDIAN_NATIVE_ORDER ? Integer.reverseBytes(value) : value);
        } else {
            unsafe.putByte(address, (byte) value);
            unsafe.putByte(address + 1, (byte) (value >>> 8));
            unsafe.putByte(address + 2, (byte) (value >>> 16));
            unsafe.putByte(address + 3, (byte) (value >>> 24));
        }
    }

    static void setLongLE(long address, long value) {
        if (UNALIGNED) {
            unsafe.putLong(address, BIG_ENDIAN_NATIVE_ORDER ? Long.reverseBytes(value) : value);
        } else {
            unsafe.putByte(address, (byte) value);
            unsafe.putByte(address + 1, (byte) (value >>> 8));
            unsafe.putByte(address + 2, (byte) (value >>> 16));
            unsafe.putByte(address + 3, (byte) (value >>> 24));
            unsafe.putByte(address + 4, (byte) (value >>> 32));
            unsafe.putByte(address + 5, (byte) (value >>> 40));
            unsafe.putByte(address + 6, (byte) (value >>> 48));
            unsafe.putByte(address + 7, (byte) (value >>> 56));
        }
    }

    private long address(int position) {
        return memoryAddress + position;
    }

    private void updateBufferAddress() {
        memoryAddress = ((DirectBuffer) super.nioBuffer).address();
    }
}
