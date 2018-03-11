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

import io.protostuff.*;
import org.jupiter.common.util.ExceptionUtil;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import static io.protostuff.StringSerializer.STRING;
import static io.protostuff.WireFormat.*;
import static org.jupiter.common.util.internal.UnsafeByteBufferUtil.*;

/**
 * jupiter
 * org.jupiter.serialization.proto.buffer
 *
 * @author jiachun.fjc
 */
class UnsafeNioBufInput implements Input {

    static final int TAG_TYPE_BITS = 3;
    static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

    static final Method byteStringWrapMethod;

    private final ByteBuffer nioBuffer;
    private int lastTag = 0;
    private int packedLimit = 0;

    /**
     * Start address of the memory buffer The memory buffer should be non-movable, which normally means that is is allocated
     * off-heap
     */
    private long memoryAddress;

    /**
     * If true, the nested messages are group-encoded
     */
    public final boolean decodeNestedMessageAsGroup;

    /**
     * An input for a ByteBuffer
     *
     * @param nioBuffer         the buffer to read from, it will not be sliced
     * @param protostuffMessage if we are parsing a protostuff (true) or protobuf (false) message
     */
    public UnsafeNioBufInput(ByteBuffer nioBuffer, boolean protostuffMessage) {
        this.nioBuffer = nioBuffer;
        this.decodeNestedMessageAsGroup = protostuffMessage;
        updateBufferAddress();
    }

    /**
     * Returns the current offset (the position).
     */
    public int currentOffset() {
        return nioBuffer.position();
    }

    /**
     * Returns the current limit (the end index).
     */
    public int currentLimit() {
        return nioBuffer.limit();
    }

    /**
     * Return true if currently reading packed field
     */
    public boolean isCurrentFieldPacked() {
        return packedLimit != 0 && packedLimit != nioBuffer.position();
    }

    /**
     * Returns the last tag.
     */
    public int getLastTag() {
        return lastTag;
    }

    /**
     * Attempt to read a field tag, returning zero if we have reached EOF. Protocol message parsers use this to read
     * tags, since a protocol message may legally end wherever a tag occurs, and zero is not a valid tag number.
     */
    public int readTag() throws IOException {
        if (!nioBuffer.hasRemaining()) {
            lastTag = 0;
            return 0;
        }

        final int tag = readRawVarInt32();
        if (tag >>> TAG_TYPE_BITS == 0) {
            // If we actually read zero, that's not a valid tag.
            throw invalidTag();
        }
        lastTag = tag;
        return tag;
    }

    /**
     * Verifies that the last call to readTag() returned the given tag value. This is used to verify that a nested group
     * ended with the correct end tag.
     *
     * @throws ProtobufException {@code value} does not match the last tag.
     */
    public void checkLastTagWas(final int value) throws ProtobufException {
        if (lastTag != value) {
            throw invalidEndTag();
        }
    }

    /**
     * Reads and discards a single field, given its tag value.
     *
     * @return {@code false} if the tag is an endgroup tag, in which case nothing is skipped. Otherwise, returns
     * {@code true}.
     */
    public boolean skipField(final int tag) throws IOException {
        switch (getTagWireType(tag)) {
            case WIRETYPE_VARINT:
                readInt32();
                return true;
            case WIRETYPE_FIXED64:
                readRawLittleEndian64();
                return true;
            case WIRETYPE_LENGTH_DELIMITED:
                final int size = readRawVarInt32();
                if (size < 0) {
                    throw negativeSize();
                }
                nioBuffer.position(nioBuffer.position() + size);
                // offset += size;
                return true;
            case WIRETYPE_START_GROUP:
                skipMessage();
                checkLastTagWas(makeTag(getTagFieldNumber(tag), WIRETYPE_END_GROUP));
                return true;
            case WIRETYPE_END_GROUP:
                return false;
            case WIRETYPE_FIXED32:
                readRawLittleEndian32();
                return true;
            default:
                throw invalidWireType();
        }
    }

    /**
     * Reads and discards an entire message. This will read either until EOF or until an endgroup tag, whichever comes
     * first.
     */
    public void skipMessage() throws IOException {
        while (true) {
            final int tag = readTag();
            if (tag == 0 || !skipField(tag)) {
                return;
            }
        }
    }

    @Override
    public <T> void handleUnknownField(int fieldNumber, Schema<T> schema) throws IOException {
        skipField(lastTag);
    }

    @Override
    public <T> int readFieldNumber(Schema<T> schema) throws IOException {
        if (!nioBuffer.hasRemaining()) {
            lastTag = 0;
            return 0;
        }

        // are we reading packed field?
        if (isCurrentFieldPacked()) {
            if (packedLimit < nioBuffer.position()) {
                throw misreportedSize();
            }

            // Return field number while reading packed field
            return lastTag >>> TAG_TYPE_BITS;
        }

        packedLimit = 0;
        final int tag = readRawVarInt32();
        final int fieldNumber = tag >>> TAG_TYPE_BITS;
        if (fieldNumber == 0) {
            if (decodeNestedMessageAsGroup &&
                    WIRETYPE_TAIL_DELIMITER == (tag & TAG_TYPE_MASK)) {
                // protostuff's tail delimiter for streaming
                // 2 options: length-delimited or tail-delimited.
                lastTag = 0;
                return 0;
            }
            // If we actually read zero, that's not a valid tag.
            throw invalidTag();
        }
        if (decodeNestedMessageAsGroup && WIRETYPE_END_GROUP == (tag & TAG_TYPE_MASK)) {
            lastTag = 0;
            return 0;
        }

        lastTag = tag;
        return fieldNumber;
    }

    /**
     * Check if this field have been packed into a length-delimited field. If so, update internal state to reflect that
     * packed fields are being read.
     */
    private void checkIfPackedField() throws IOException {
        // Do we have the start of a packed field?
        if (packedLimit == 0 && getTagWireType(lastTag) == WIRETYPE_LENGTH_DELIMITED) {
            final int length = readRawVarInt32();
            if (length < 0) {
                throw negativeSize();
            }

            if (nioBuffer.position() + length > nioBuffer.limit()) {
                throw misreportedSize();
            }

            this.packedLimit = nioBuffer.position() + length;
        }
    }

    /**
     * Read a {@code double} field value from the internal buffer.
     */
    @Override
    public double readDouble() throws IOException {
        checkIfPackedField();
        return Double.longBitsToDouble(readRawLittleEndian64());
    }

    /**
     * Read a {@code float} field value from the internal buffer.
     */
    @Override
    public float readFloat() throws IOException {
        checkIfPackedField();
        return Float.intBitsToFloat(readRawLittleEndian32());
    }

    /**
     * Read a {@code uint64} field value from the internal buffer.
     */
    @Override
    public long readUInt64() throws IOException {
        checkIfPackedField();
        return readRawVarInt64();
    }

    /**
     * Read an {@code int64} field value from the internal buffer.
     */
    @Override
    public long readInt64() throws IOException {
        checkIfPackedField();
        return readRawVarInt64();
    }

    /**
     * Read an {@code int32} field value from the internal buffer.
     */
    @Override
    public int readInt32() throws IOException {
        checkIfPackedField();
        return readRawVarInt32();
    }

    /**
     * Read a {@code fixed64} field value from the internal buffer.
     */
    @Override
    public long readFixed64() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian64();
    }

    /**
     * Read a {@code fixed32} field value from the internal buffer.
     */
    @Override
    public int readFixed32() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian32();
    }

    /**
     * Read a {@code bool} field value from the internal buffer.
     */
    @Override
    public boolean readBool() throws IOException {
        checkIfPackedField();
        int position = nioBuffer.position();
        boolean result = getByte(address(position)) != 0;
        nioBuffer.position(position + 1);
        return result;
    }

    /**
     * Read a {@code uint32} field value from the internal buffer.
     */
    @Override
    public int readUInt32() throws IOException {
        checkIfPackedField();
        return readRawVarInt32();
    }

    /**
     * Read an enum field value from the internal buffer. Caller is responsible for converting the numeric value to an
     * actual enum.
     */
    @Override
    public int readEnum() throws IOException {
        checkIfPackedField();
        return readRawVarInt32();
    }

    /**
     * Read an {@code sfixed32} field value from the internal buffer.
     */
    @Override
    public int readSFixed32() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian32();
    }

    /**
     * Read an {@code sfixed64} field value from the internal buffer.
     */
    @Override
    public long readSFixed64() throws IOException {
        checkIfPackedField();
        return readRawLittleEndian64();
    }

    /**
     * Read an {@code sint32} field value from the internal buffer.
     */
    @Override
    public int readSInt32() throws IOException {
        checkIfPackedField();
        final int n = readRawVarInt32();
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * Read an {@code sint64} field value from the internal buffer.
     */
    @Override
    public long readSInt64() throws IOException {
        checkIfPackedField();
        final long n = readRawVarInt64();
        return (n >>> 1) ^ -(n & 1);
    }

    @Override
    public String readString() throws IOException {
        final int length = readRawVarInt32();
        if (length < 0) {
            throw negativeSize();
        }

        if (nioBuffer.remaining() < length) {
            throw misreportedSize();
        }

        String result;
        int position = nioBuffer.position();
        if (nioBuffer.hasArray()) {
            result = STRING.deser(nioBuffer.array(), nioBuffer.arrayOffset() + position, length);
        } else {
            byte[] tmp = new byte[length];
            getBytes(address(position), tmp, 0, length);
            result = STRING.deser(tmp);
        }
        nioBuffer.position(position + length);
        return result;
    }

    @SuppressWarnings("all")
    @Override
    public ByteString readBytes() throws IOException {
        try {
            return (ByteString) byteStringWrapMethod.invoke(null, readByteArray());
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }
        return null; // never get here
    }

    @Override
    public void readBytes(final ByteBuffer bb) throws IOException {
        final int length = readRawVarInt32();
        if (length < 0) {
            throw negativeSize();
        }

        if (nioBuffer.remaining() < length) {
            throw misreportedSize();
        }

        bb.put(nioBuffer);
    }

    @Override
    public byte[] readByteArray() throws IOException {
        final int length = readRawVarInt32();
        if (length < 0) {
            throw negativeSize();
        }

        if (nioBuffer.remaining() < length) {
            throw misreportedSize();
        }

        final byte[] copy = new byte[length];
        int position = nioBuffer.position();
        getBytes(address(position), copy, 0, length);
        nioBuffer.position(position + length);
        return copy;
    }

    @Override
    public <T> T mergeObject(T value, final Schema<T> schema) throws IOException {
        if (decodeNestedMessageAsGroup) {
            return mergeObjectEncodedAsGroup(value, schema);
        }

        final int length = readRawVarInt32();
        if (length < 0) {
            throw negativeSize();
        }

        if (nioBuffer.remaining() < length) {
            throw misreportedSize();
        }

        ByteBuffer dup = nioBuffer.slice();
        dup.limit(length);

        if (value == null) {
            value = schema.newMessage();
        }
        ByteBufferInput nestedInput = new ByteBufferInput(dup, false);
        schema.mergeFrom(nestedInput, value);
        if (!schema.isInitialized(value)) {
            throw new UninitializedMessageException(value, schema);
        }
        nestedInput.checkLastTagWas(0);

        nioBuffer.position(nioBuffer.position() + length);
        return value;
    }

    private <T> T mergeObjectEncodedAsGroup(T value, final Schema<T> schema) throws IOException {
        if (value == null) {
            value = schema.newMessage();
        }
        schema.mergeFrom(this, value);
        if (!schema.isInitialized(value)) {
            throw new UninitializedMessageException(value, schema);
        }
        // handling is in #readFieldNumber
        checkLastTagWas(0);
        return value;
    }

    /**
     * Reads a var int 32 from the internal byte buffer.
     */
    public int readRawVarInt32() throws IOException {
        int position = nioBuffer.position();
        byte tmp = getByte(address(position++));
        if (tmp >= 0) {
            nioBuffer.position(position);
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = getByte(address(position++))) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = getByte(address(position++))) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = getByte(address(position++))) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = getByte(address(position++))) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (getByte(address(position++)) >= 0) {
                                nioBuffer.position(position);
                                return result;
                            }
                        }
                        throw malformedVarint();
                    }
                }
            }
        }
        nioBuffer.position(position);
        return result;
    }

    /**
     * Reads a var int 64 from the internal byte buffer.
     */
    public long readRawVarInt64() throws IOException {
        int shift = 0;
        long result = 0;
        int position = nioBuffer.position();
        while (shift < 64) {
            final byte b = getByte(address(position++));
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                nioBuffer.position(position);
                return result;
            }
            shift += 7;
        }
        throw malformedVarint();
    }

    /**
     * Read a 32-bit little-endian integer from the internal buffer.
     */
    public int readRawLittleEndian32() throws IOException {
        int position = nioBuffer.position();
        int result = getIntLE(address(position));
        nioBuffer.position(position + 4);
        return result;
    }

    /**
     * Read a 64-bit little-endian integer from the internal byte buffer.
     */
    public long readRawLittleEndian64() throws IOException {
        int position = nioBuffer.position();
        long result = getLongLE(address(position));
        nioBuffer.position(position + 8);
        return result;
    }

    @Override
    public void transferByteRangeTo(Output output, boolean utf8String, int fieldNumber,
                                    boolean repeated) throws IOException {
        final int length = readRawVarInt32();
        if (length < 0) {
            throw negativeSize();
        }

        if (utf8String) {
            // if it is a UTF string, we have to call the writeByteRange.

            if (nioBuffer.hasArray()) {
                output.writeByteRange(true, fieldNumber, nioBuffer.array(),
                        nioBuffer.arrayOffset() + nioBuffer.position(), length, repeated);
                nioBuffer.position(nioBuffer.position() + length);
            } else {
                byte[] bytes = new byte[length];
                int position = nioBuffer.position();
                getBytes(address(position), bytes, 0, length);
                nioBuffer.position(position + length);
                output.writeByteRange(true, fieldNumber, bytes, 0, bytes.length, repeated);
            }
        } else {
            // Do the potentially vastly more efficient potential splice call.
            if (nioBuffer.remaining() < length) {
                throw misreportedSize();
            }

            ByteBuffer dup = nioBuffer.slice();
            dup.limit(length);

            output.writeBytes(fieldNumber, dup, repeated);

            nioBuffer.position(nioBuffer.position() + length);
        }
    }

    /**
     * Reads a byte array/ByteBuffer value.
     */
    @Override
    public ByteBuffer readByteBuffer() throws IOException {
        return ByteBuffer.wrap(readByteArray());
    }

    private long address(int position) {
        return memoryAddress + position;
    }

    private void updateBufferAddress() {
        memoryAddress = ((DirectBuffer) nioBuffer).address();
    }

    static ProtobufException misreportedSize() {
        return new ProtobufException(
                "CodedInput encountered an embedded string or bytes " +
                        "that misreported its size.");
    }

    static ProtobufException negativeSize() {
        return new ProtobufException(
                "CodedInput encountered an embedded string or message " +
                        "which claimed to have negative size.");
    }

    static ProtobufException malformedVarint() {
        return new ProtobufException(
                "CodedInput encountered a malformed varint.");
    }

    static ProtobufException invalidTag() {
        return new ProtobufException(
                "Protocol message contained an invalid tag (zero).");
    }

    static ProtobufException invalidEndTag() {
        return new ProtobufException(
                "Protocol message end-group tag did not match expected tag.");
    }

    static ProtobufException invalidWireType() {
        return new ProtobufException(
                "Protocol message tag had invalid wire type.");
    }

    static ProtobufException recursionLimitExceeded() {
        return new ProtobufException(
                "Protocol message had too many levels of nesting.  May be malicious.  " +
                        "Use CodedInput.setRecursionLimit() to increase the depth limit.");
    }

    static ProtobufException sizeLimitExceeded() {
        return new ProtobufException(
                "Protocol message was too large.  May be malicious.  " +
                        "Use CodedInput.setSizeLimit() to increase the size limit.");
    }

    static {
        try {
            byteStringWrapMethod = ByteString.class.getDeclaredMethod("wrap", byte[].class);
            byteStringWrapMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }
}
