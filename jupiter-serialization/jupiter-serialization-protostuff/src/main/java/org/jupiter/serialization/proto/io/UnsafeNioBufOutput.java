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
package org.jupiter.serialization.proto.io;

import java.io.IOException;

import org.jupiter.common.util.internal.UnsafeDirectBufferUtil;
import org.jupiter.common.util.internal.UnsafeUtf8Util;
import org.jupiter.common.util.internal.UnsafeUtil;
import org.jupiter.serialization.io.OutputBuf;

import static io.protostuff.WireFormat.WIRETYPE_LENGTH_DELIMITED;
import static io.protostuff.WireFormat.makeTag;

/**
 * jupiter
 * org.jupiter.serialization.proto.io
 *
 * @author jiachun.fjc
 */
class UnsafeNioBufOutput extends NioBufOutput {

    /**
     * Start address of the memory buffer The memory buffer should be non-movable, which normally means that is is allocated
     * off-heap
     */
    private long memoryAddress;

    UnsafeNioBufOutput(OutputBuf outputBuf, int minWritableBytes, int maxCapacity) {
        super(outputBuf, minWritableBytes, maxCapacity);
        updateBufferAddress();
    }

    @Override
    public void writeString(int fieldNumber, CharSequence value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED));

        // UTF-8 byte length of the string is at least its UTF-16 code unit length (value.length()),
        // and at most 3 times of it. We take advantage of this in both branches below.
        int minLength = value.length();
        int maxLength = minLength * UnsafeUtf8Util.MAX_BYTES_PER_CHAR;
        int minLengthVarIntSize = VarInts.computeRawVarInt32Size(minLength);
        int maxLengthVarIntSize = VarInts.computeRawVarInt32Size(maxLength);
        if (minLengthVarIntSize == maxLengthVarIntSize) {
            int position = nioBuffer.position();

            ensureCapacity(maxLengthVarIntSize + maxLength);

            // Save the current position and increment past the length field. We'll come back
            // and write the length field after the encoding is complete.
            int stringStartPos = position + maxLengthVarIntSize;
            nioBuffer.position(stringStartPos);

            // Encode the string.
            UnsafeUtf8Util.encodeUtf8Direct(value, nioBuffer);

            // Write the length and advance the position.
            int length = nioBuffer.position() - stringStartPos;
            nioBuffer.position(position);
            writeVarInt32(length);
            nioBuffer.position(stringStartPos + length);
        } else {
            // Calculate and write the encoded length.
            int length = UnsafeUtf8Util.encodedLength(value);
            writeVarInt32(length);

            ensureCapacity(length);

            // Write the string and advance the position.
            UnsafeUtf8Util.encodeUtf8Direct(value, nioBuffer);
        }
    }

    // VarInt是一种变长的的数字编码方式, 用字节表示数字, 值越小的数字, 占用的字节越少
    // 通过减少表示数字的字节数, 从而进行数据压缩
    //
    // 每个字节的最高位都是一个标志:
    // 如果是1: 表示后续的字节也是该数字的一部分
    // 如果是0: 表示这是最后一个字节, 剩余7位都是用来表示数字
    @Override
    protected void writeVarInt32(int value) throws IOException {
//        ensureCapacity(5);
//        int position = nioBuffer.position();
//        while (true) {
//            if ((value & ~0x7F) == 0) {
//                // 3. 这是最后一次取出, 最高位是0, 构成一个字节, 此时的字节串就是VarInt编码后的字节
//                UnsafeDirectBufferUtil.setByte(address(position++), (byte) value);
//                nioBuffer.position(position);
//                return;
//            } else {
//                // 1. 取出字节串末尾7位, 并将最高位设置为1(与0x80按位或), 构成一个字节
//                UnsafeDirectBufferUtil.setByte(address(position++), (byte) ((value & 0x7F) | 0x80));
//                // 2. 将字节串整体右移7位, 继续从字节串末尾取7位, 取完为止
//                value >>>= 7;
//            }
//        }
//
// 以下实现同上面代码功能上没区别, 只是把多次setByte聚合为一次setShort/setInt
//
        ensureCapacity(5);
        int position = nioBuffer.position();
        if ((value & (~0 << 7)) == 0) {
            // size == 1
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) value);
        } else if ((value & (~0 << 14)) == 0) {
            // size == 2
            UnsafeDirectBufferUtil.setShort(address(position),
                    (((value & 0x7F) | 0x80) << 8) | (value >>> 7));
            position += 2;
        } else if ((value & (~0 << 21)) == 0) {
            // size == 3
            UnsafeDirectBufferUtil.setShort(address(position),
                    (((value & 0x7F) | 0x80) << 8) | ((value >>> 7 & 0x7F) | 0x80));
            position += 2;
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) (value >>> 14));
        } else if ((value & (~0 << 28)) == 0) {
            // size == 4
            UnsafeDirectBufferUtil.setInt(address(position),
                    (((value & 0x7F) | 0x80) << 24)
                            | (((value >>> 7 & 0x7F) | 0x80) << 16)
                            | (((value >>> 14 & 0x7F) | 0x80) << 8)
                            | (value >>> 21));
            position += 4;
        } else {
            // size == 5
            UnsafeDirectBufferUtil.setInt(address(position),
                    (((value & 0x7F) | 0x80) << 24)
                            | (((value >>> 7 & 0x7F) | 0x80) << 16)
                            | (((value >>> 14 & 0x7F) | 0x80) << 8)
                            | ((value >>> 21 & 0x7F) | 0x80));
            position += 4;
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) (value >>> 28));
        }
        nioBuffer.position(position);
    }

    @Override
    protected void writeVarInt64(long value) throws IOException {
//        ensureCapacity(10);
//        int position = nioBuffer.position();
//        while (true) {
//            if ((value & ~0x7FL) == 0) {
//                UnsafeDirectBufferUtil.setByte(address(position++), (byte) value);
//                nioBuffer.position(position);
//                return;
//            } else {
//                UnsafeDirectBufferUtil.setByte(address(position++), (byte) (((int) value & 0x7F) | 0x80));
//                value >>>= 7;
//            }
//        }
//
// 以下实现同上面代码功能上没区别, 只是把多次setByte聚合为一次setShort/setInt/setLong
//
        ensureCapacity(10);
        int position = nioBuffer.position();
        // Handle two popular special cases up front ...
        if ((value & (~0L << 7)) == 0) {
            // size == 1
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) value);
        } else if (value < 0L) {
            // size == 10
            UnsafeDirectBufferUtil.setLong(address(position),
                    (((value & 0x7F) | 0x80) << 56)
                            | (((value >>> 7 & 0x7F) | 0x80) << 48)
                            | (((value >>> 14 & 0x7F) | 0x80) << 40)
                            | (((value >>> 21 & 0x7F) | 0x80) << 32)
                            | (((value >>> 28 & 0x7F) | 0x80) << 24)
                            | (((value >>> 35 & 0x7F) | 0x80) << 16)
                            | (((value >>> 42 & 0x7F) | 0x80) << 8)
                            | ((value >>> 49 & 0x7F) | 0x80));
            position += 8;
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) (value >>> 56) & 0x7F) | 0x80) << 8) | (int) (value >>> 63));
            position += 2;
        }
        // ... leaving us with 8 remaining [2, 3, 4, 5, 6, 7, 8, 9]
        else if ((value & (~0L << 14)) == 0) {
            // size == 2
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) value & 0x7F) | 0x80) << 8) | (byte) (value >>> 7));
            position += 2;
        } else if ((value & (~0L << 21)) == 0) {
            // size == 3
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) value & 0x7F) | 0x80) << 8) | (((int) value >>> 7 & 0x7F) | 0x80));
            position += 2;
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) (value >>> 14));
        } else if ((value & (~0L << 28)) == 0) {
            // size == 4
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | ((int) (value >>> 21)));
            position += 4;
        } else if ((value & (~0L << 35)) == 0) {
            // size == 5
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80));
            position += 4;
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) (value >>> 28));
        } else if ((value & (~0L << 42)) == 0) {
            // size == 6
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80)
            );
            position += 4;
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) (value >>> 28) & 0x7F) | 0x80) << 8) | (int) (value >>> 35));
            position += 2;
        } else if ((value & (~0L << 49)) == 0) {
            // size == 7
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80)
            );
            position += 4;
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) (value >>> 28) & 0x7F) | 0x80) << 8) | (((int) (value >>> 35) & 0x7F) | 0x80));
            position += 2;
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) (value >>> 42));
        } else if ((value & (~0L << 56)) == 0) {
            // size == 8
            UnsafeDirectBufferUtil.setLong(address(position),
                    (((value & 0x7F) | 0x80) << 56)
                            | (((value >>> 7 & 0x7F) | 0x80) << 48)
                            | (((value >>> 14 & 0x7F) | 0x80) << 40)
                            | (((value >>> 21 & 0x7F) | 0x80) << 32)
                            | (((value >>> 28 & 0x7F) | 0x80) << 24)
                            | (((value >>> 35 & 0x7F) | 0x80) << 16)
                            | (((value >>> 42 & 0x7F) | 0x80) << 8)
                            | (value >>> 49));
            position += 8;
        } else {
            // size == 9 (value & (~0L << 63)) == 0
            UnsafeDirectBufferUtil.setLong(address(position),
                    (((value & 0x7F) | 0x80) << 56)
                            | (((value >>> 7 & 0x7F) | 0x80) << 48)
                            | (((value >>> 14 & 0x7F) | 0x80) << 40)
                            | (((value >>> 21 & 0x7F) | 0x80) << 32)
                            | (((value >>> 28 & 0x7F) | 0x80) << 24)
                            | (((value >>> 35 & 0x7F) | 0x80) << 16)
                            | (((value >>> 42 & 0x7F) | 0x80) << 8)
                            | ((value >>> 49 & 0x7F) | 0x80));
            position += 8;
            UnsafeDirectBufferUtil.setByte(address(position++), (byte) (value >>> 56));
        }
        nioBuffer.position(position);
    }

    @Override
    protected void writeInt32LE(int value) throws IOException {
        ensureCapacity(4);
        int position = nioBuffer.position();
        UnsafeDirectBufferUtil.setIntLE(address(position), value);
        nioBuffer.position(position + 4);
    }

    @Override
    protected void writeInt64LE(long value) throws IOException {
        ensureCapacity(8);
        int position = nioBuffer.position();
        UnsafeDirectBufferUtil.setLongLE(address(position), value);
        nioBuffer.position(position + 8);
    }

    @Override
    protected void writeByte(byte value) throws IOException {
        ensureCapacity(1);
        int position = nioBuffer.position();
        UnsafeDirectBufferUtil.setByte(address(position), value);
        nioBuffer.position(position + 1);
    }

    @Override
    protected void writeByteArray(byte[] value, int offset, int length) throws IOException {
        ensureCapacity(length);
        int position = nioBuffer.position();
        UnsafeDirectBufferUtil.setBytes(address(position), value, offset, length);
        nioBuffer.position(position + length);
    }

    @Override
    protected void ensureCapacity(int required) throws ProtocolException {
        if (nioBuffer.remaining() < required) {
            int position = nioBuffer.position();

            while (capacity - position < required) {
                if (capacity == maxCapacity) {
                    throw new ProtocolException(
                            "Buffer overflow. Available: " + (capacity - position) + ", required: " + required);
                }
                capacity = Math.min(capacity << 1, maxCapacity);
                if (capacity < 0) {
                    capacity = maxCapacity;
                }
            }

            nioBuffer = outputBuf.nioByteBuffer(capacity - position);
            capacity = nioBuffer.limit();
            // Need to update the direct buffer's memory address
            updateBufferAddress();
        }
    }

    private void updateBufferAddress() {
        memoryAddress = UnsafeUtil.addressOffset(nioBuffer);
    }

    private long address(int position) {
        return memoryAddress + position;
    }
}
