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

package org.jupiter.serialization.proto;

import io.protostuff.ByteString;
import io.protostuff.IntSerializer;
import io.protostuff.Output;
import io.protostuff.Schema;
import org.jupiter.common.util.ExceptionUtil;
import org.jupiter.serialization.OutputBuf;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import static io.protostuff.ProtobufOutput.encodeZigZag32;
import static io.protostuff.ProtobufOutput.encodeZigZag64;
import static io.protostuff.WireFormat.*;

/**
 * jupiter
 * org.jupiter.serialization.proto
 *
 * @author jiachun.fjc
 */
public class NioBufOutput implements Output {

    private static final Method byteStringGetBytesMethod;

    private final OutputBuf outputBuf;
    private ByteBuffer nioBuffer;
    private int capacity;

    public NioBufOutput(OutputBuf outputBuf, int minWritableBytes) {
        this.outputBuf = outputBuf;
        nioBuffer = outputBuf.nioByteBuffer(minWritableBytes);
        capacity = nioBuffer.remaining();
    }

    @Override
    public void writeInt32(int fieldNumber, int value, boolean repeated) throws IOException {
        if (value < 0) {
            writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
            writeVarInt64(value);
        } else {
            writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
            writeVarInt32(value);
        }
    }

    @Override
    public void writeUInt32(int fieldNumber, int value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
        writeVarInt32(value);
    }

    @Override
    public void writeSInt32(int fieldNumber, int value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
        writeVarInt32(encodeZigZag32(value));
    }

    @Override
    public void writeFixed32(int fieldNumber, int value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_FIXED32));
        writeInt32LE(value);
    }

    @Override
    public void writeSFixed32(int fieldNumber, int value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_FIXED32));
        writeInt32LE(value);
    }

    @Override
    public void writeInt64(int fieldNumber, long value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
        writeVarInt64(value);
    }

    @Override
    public void writeUInt64(int fieldNumber, long value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
        writeVarInt64(value);
    }

    @Override
    public void writeSInt64(int fieldNumber, long value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
        writeVarInt64(encodeZigZag64(value));
    }

    @Override
    public void writeFixed64(int fieldNumber, long value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_FIXED64));
        writeInt64LE(value);
    }

    @Override
    public void writeSFixed64(int fieldNumber, long value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_FIXED64));
        writeInt64LE(value);
    }

    @Override
    public void writeFloat(int fieldNumber, float value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_FIXED32));
        writeInt32LE(Float.floatToRawIntBits(value));
    }

    @Override
    public void writeDouble(int fieldNumber, double value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_FIXED64));
        writeInt64LE(Double.doubleToRawLongBits(value));
    }

    @Override
    public void writeBool(int fieldNumber, boolean value, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_VARINT));
        writeByte(value ? (byte) 0x01 : 0x00);
    }

    @Override
    public void writeEnum(int fieldNumber, int value, boolean repeated) throws IOException {
        writeInt32(fieldNumber, value, repeated);
    }

    @Override
    public void writeString(int fieldNumber, CharSequence value, boolean repeated) throws IOException {
        // TODO the original implementation is a lot more complex, is this compatible?
        byte[] strBytes = value.toString().getBytes("UTF-8");
        writeByteArray(fieldNumber, strBytes, repeated);
    }

    @Override
    public void writeBytes(int fieldNumber, ByteString value, boolean repeated) throws IOException {
        try {
            writeByteArray(fieldNumber, (byte[]) byteStringGetBytesMethod.invoke(value), repeated);
        } catch (Throwable t) {
            ExceptionUtil.throwException(t);
        }
    }

    @Override
    public void writeByteArray(int fieldNumber, byte[] value, boolean repeated) throws IOException {
        writeByteRange(false, fieldNumber, value, 0, value.length, repeated);
    }

    @Override
    public void writeByteRange(boolean utf8String, int fieldNumber, byte[] value, int offset, int length, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED));
        writeVarInt32(length);
        writeByteArray(value, offset, length);
    }

    @Override
    public <T> void writeObject(int fieldNumber, T value, Schema<T> schema, boolean repeated) throws IOException {
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_START_GROUP));
        schema.writeTo(this, value);
        writeVarInt32(makeTag(fieldNumber, WIRETYPE_END_GROUP));
    }

    @Override
    public void writeBytes(int fieldNumber, ByteBuffer value, boolean repeated) throws IOException {
        writeByteRange(false, fieldNumber, value.array(), value.arrayOffset() + value.position(),
                value.remaining(), repeated);
    }

    private void writeVarInt32(int value) throws IOException {
        byte[] buf = new byte[5];
        int locPtr = 0;
        while (true) {
            if ((value & ~0x7F) == 0) {
                buf[locPtr++] = (byte) value;
                // thing;
                ensureCapacity(locPtr);
                nioBuffer.put(buf, 0, locPtr);
                return;
            } else {
                buf[locPtr++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    private void writeVarInt64(long value) throws IOException {
        byte[] buf = new byte[10];
        int locPtr = 0;
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buf[locPtr++] = (byte) value;
                ensureCapacity(locPtr);
                nioBuffer.put(buf, 0, locPtr);
                return;
            } else {
                buf[locPtr++] = (byte) (((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    private void writeInt32LE(final int value) throws IOException {
        ensureCapacity(4);
        IntSerializer.writeInt32LE(value, nioBuffer);
    }

    private void writeInt64LE(final long value) throws IOException {
        ensureCapacity(8);
        IntSerializer.writeInt64LE(value, nioBuffer);
    }

    private void writeByte(final byte value) throws IOException {
        ensureCapacity(1);
        nioBuffer.put(value);
    }

    private void writeByteArray(final byte[] value,
                                final int offset, final int length) throws IOException {
        ensureCapacity(length);
        nioBuffer.put(value, offset, length);
    }

    private void ensureCapacity(int required) {
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
        }
    }

    static {
        try {
            byteStringGetBytesMethod = ByteString.class.getDeclaredMethod("getBytes");
            byteStringGetBytesMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }
}
