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
package org.jupiter.common.util;

/**
 * Static utility methods pertaining to {@code byte} primitives.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class Bytes {

    /**
     * Returns a int by decoding the specified sub array of bytes.
     */
    public static int bytes2Int(byte[] bytes, int start, int length) {
        Requires.requireTrue(length > 0 && length <= 4, "invalid length: " + length); // @author jeremy
        int sum = 0;
        int end = start + length;
        for (int i = start; i < end; i++) {
            int n = bytes[i] & 0xff;
            n <<= (--length) * 8;
            sum |= n; // @author okou
        }
        return sum;
    }

    /**
     * Encodes this {@code int} into a sequence of bytes, storing the result
     * into a new byte array.
     */
    public static byte[] int2Bytes(int value, int length) {
        Requires.requireTrue(length > 0 && length <= 4, "invalid length: " + length); // @author jeremy
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[length - i - 1] = (byte) ((value >> 8 * i) & 0xff);
        }
        return bytes;
    }

    /**
     * Constructs a new {@code String} by decoding the specified sub array of
     * bytes using the platform's default charset.
     */
    public static String bytes2String(byte[] bytes, int start, int length) {
        return new String(bytes, start, length);
    }

    /**
     * Encodes this {@code String} into a sequence of bytes using the
     * platform's default charset, storing the result into a new byte array.
     */
    public static byte[] string2Bytes(String str) {
        return str.getBytes();
    }

    /**
     * Replaces bytes in the specified sub array.
     */
    public static byte[] replace(byte[] originalBytes, int offset, int length, byte[] replaceBytes) {
        byte[] newBytes = new byte[originalBytes.length + (replaceBytes.length - length)];

        System.arraycopy(originalBytes, 0, newBytes, 0, offset);
        System.arraycopy(replaceBytes, 0, newBytes, offset, replaceBytes.length);
        System.arraycopy(originalBytes, offset + length, newBytes, offset + replaceBytes.length, originalBytes.length - offset - length);

        return newBytes;
    }

    /**
     * Returns {@code true} if {@code target} is present as an element anywhere
     * in {@code array}.
     */
    public static boolean contains(byte[] array, byte target) {
        for (byte value : array) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    private Bytes() {}
}
