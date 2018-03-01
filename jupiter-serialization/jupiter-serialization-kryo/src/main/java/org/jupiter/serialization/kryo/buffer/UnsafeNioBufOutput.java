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

package org.jupiter.serialization.kryo.buffer;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.util.Util;
import org.jupiter.serialization.OutputBuf;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteOrder;

import static com.esotericsoftware.kryo.util.UnsafeUtil.*;

/**
 * jupiter
 * org.jupiter.serialization.kryo.buffer
 *
 * @author jiachun.fjc
 */
class UnsafeNioBufOutput extends NioBufOutput {

    private final static boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);

    /**
     * Start address of the memory buffer The memory buffer should be non-movable, which normally means that is is allocated
     * off-heap
     */
    private long address;

    public UnsafeNioBufOutput(OutputBuf outputBuf, int minWritableBytes) {
        super(outputBuf, minWritableBytes);
        updateBufferAddress();
    }

    @Override
    final public void writeInt(int value) throws KryoException {
        require(4);
        unsafe().putInt(address + position, value);
        position += 4;
        niobuffer.position(position);
    }

    @Override
    final public void writeFloat(float value) throws KryoException {
        require(4);
        unsafe().putFloat(address + position, value);
        position += 4;
        niobuffer.position(position);
    }

    @Override
    final public void writeShort(int value) throws KryoException {
        require(2);
        unsafe().putShort(address + position, (short) value);
        position += 2;
        niobuffer.position(position);
    }

    @Override
    final public void writeLong(long value) throws KryoException {
        require(8);
        unsafe().putLong(address + position, value);
        position += 8;
        niobuffer.position(position);
    }

    @Override
    final public void writeDouble(double value) throws KryoException {
        require(8);
        unsafe().putDouble(address + position, value);
        position += 8;
        niobuffer.position(position);
    }

    @Override
    final public int writeInt(int value, boolean optimizePositive) throws KryoException {
        if (!varIntsEnabled) {
            writeInt(value);
            return 4;
        } else {
            return writeVarInt(value, optimizePositive);
        }
    }

    @Override
    final public int writeLong(long value, boolean optimizePositive) throws KryoException {
        if (!varIntsEnabled) {
            writeLong(value);
            return 8;
        } else {
            return writeVarLong(value, optimizePositive);
        }
    }

    @SuppressWarnings("all")
    @Override
    final public int writeVarInt(int val, boolean optimizePositive) throws KryoException {
        long value = val;
        if (!optimizePositive) {
            value = (value << 1) ^ (value >> 31);
        }
        long varInt;

        varInt = (value & 0x7F);

        value >>>= 7;

        if (value == 0) {
            writeByte((byte) varInt);
            return 1;
        }

        varInt |= 0x80;
        varInt |= ((value & 0x7F) << 8);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianInt((int) varInt);
            position -= 2;
            niobuffer.position(position);
            return 2;
        }

        varInt |= (0x80 << 8);
        varInt |= ((value & 0x7F) << 16);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianInt((int) varInt);
            position -= 1;
            niobuffer.position(position);
            return 3;
        }

        varInt |= (0x80 << 16);
        varInt |= ((value & 0x7F) << 24);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianInt((int) varInt);
            position -= 0;
            niobuffer.position(position);
            return 4;
        }

        varInt |= (0x80 << 24);
        varInt |= ((value & 0x7F) << 32);
        varInt &= 0xFFFFFFFFL;
        writeLittleEndianLong(varInt);
        position -= 3;
        niobuffer.position(position);
        return 5;
    }

    @SuppressWarnings("all")
    @Override
    final public int writeVarLong(long value, boolean optimizePositive) throws KryoException {
        if (!optimizePositive) {
            value = (value << 1) ^ (value >> 63);
        }
        int varInt;

        varInt = (int) (value & 0x7F);

        value >>>= 7;

        if (value == 0) {
            write(varInt);
            return 1;
        }

        varInt |= 0x80;
        varInt |= ((value & 0x7F) << 8);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianInt(varInt);
            position -= 2;
            niobuffer.position(position);
            return 2;
        }

        varInt |= (0x80 << 8);
        varInt |= ((value & 0x7F) << 16);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianInt(varInt);
            position -= 1;
            niobuffer.position(position);
            return 3;
        }

        varInt |= (0x80 << 16);
        varInt |= ((value & 0x7F) << 24);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianInt(varInt);
            position -= 0;
            niobuffer.position(position);
            return 4;
        }

        varInt |= (0x80 << 24);
        long varLong = (varInt & 0xFFFFFFFFL) | ((value & 0x7F) << 32);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianLong(varLong);
            position -= 3;
            niobuffer.position(position);
            return 5;
        }

        varLong |= (0x80L << 32);
        varLong |= ((value & 0x7F) << 40);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianLong(varLong);
            position -= 2;
            niobuffer.position(position);
            return 6;
        }

        varLong |= (0x80L << 40);
        varLong |= ((value & 0x7F) << 48);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianLong(varLong);
            position -= 1;
            niobuffer.position(position);
            return 7;
        }

        varLong |= (0x80L << 48);
        varLong |= ((value & 0x7F) << 56);

        value >>>= 7;

        if (value == 0) {
            writeLittleEndianLong(varLong);
            return 8;
        }

        varLong |= (0x80L << 56);
        writeLittleEndianLong(varLong);
        write((byte) ((value & 0x7F)));
        return 9;
    }

    private void writeLittleEndianInt(int val) {
        if (IS_LITTLE_ENDIAN) {
            writeInt(val);
        } else {
            writeInt(Util.swapInt(val));
        }
    }

    private void writeLittleEndianLong(long val) {
        if (IS_LITTLE_ENDIAN) {
            writeLong(val);
        } else {
            writeLong(Util.swapLong(val));
        }
    }

    // Methods implementing bulk operations on arrays of primitive types

    @Override
    public final void writeInts(int[] object, boolean optimizePositive) throws KryoException {
        if (!varIntsEnabled) {
            int bytesToCopy = object.length << 2;
            writeBytes(object, intArrayBaseOffset, 0, bytesToCopy);
        } else {
            super.writeInts(object, optimizePositive);
        }
    }

    @Override
    public final void writeLongs(long[] object, boolean optimizePositive) throws KryoException {
        if (!varIntsEnabled) {
            int bytesToCopy = object.length << 3;
            writeBytes(object, longArrayBaseOffset, 0, bytesToCopy);
        } else {
            super.writeLongs(object, optimizePositive);
        }
    }

    @Override
    public final void writeInts(int[] object) throws KryoException {
        int bytesToCopy = object.length << 2;
        writeBytes(object, intArrayBaseOffset, 0, bytesToCopy);
    }

    @Override
    public final void writeLongs(long[] object) throws KryoException {
        int bytesToCopy = object.length << 3;
        writeBytes(object, longArrayBaseOffset, 0, bytesToCopy);
    }

    @Override
    public final void writeFloats(float[] object) throws KryoException {
        int bytesToCopy = object.length << 2;
        writeBytes(object, floatArrayBaseOffset, 0, bytesToCopy);
    }

    @Override
    public final void writeShorts(short[] object) throws KryoException {
        int bytesToCopy = object.length << 1;
        writeBytes(object, shortArrayBaseOffset, 0, bytesToCopy);
    }

    @Override
    public final void writeChars(char[] object) throws KryoException {
        int bytesToCopy = object.length << 1;
        writeBytes(object, charArrayBaseOffset, 0, bytesToCopy);
    }

    @Override
    public final void writeDoubles(double[] object) throws KryoException {
        int bytesToCopy = object.length << 3;
        writeBytes(object, doubleArrayBaseOffset, 0, bytesToCopy);
    }

    public final void writeBytes(Object obj, long offset, long count) throws KryoException {
        writeBytes(obj, 0, offset, count);
    }

    private void writeBytes(Object srcArray, long srcArrayTypeOffset, long srcOffset, long count) throws KryoException {
        int copyCount = Math.min(capacity - position, (int) count);

        while (true) {
            unsafe().copyMemory(srcArray, srcArrayTypeOffset + srcOffset, null, address + position, copyCount);
            position += copyCount;
            count -= copyCount;
            if (count == 0) {
                niobuffer.position(position);
                return;
            }
            srcOffset += copyCount;
            copyCount = Math.min(capacity, (int) count);
            require(copyCount);
        }
    }

    private void updateBufferAddress() {
        address = ((DirectBuffer) super.niobuffer).address();
    }
}
