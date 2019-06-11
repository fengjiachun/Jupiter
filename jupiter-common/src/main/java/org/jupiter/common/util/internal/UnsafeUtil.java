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
package org.jupiter.common.util.internal;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

/**
 * For the {@link sun.misc.Unsafe} access.
 * <p>
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("ConstantConditions")
public final class UnsafeUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UnsafeUtil.class);

    private static final Object         UNSAFE                    = getUnsafe0();

    private static final UnsafeAccessor UNSAFE_ACCESSOR           = getUnsafeAccessor0();

    private static final long           BYTE_ARRAY_BASE_OFFSET    = arrayBaseOffset(byte[].class);
    // Micro-optimization: we can assume a scale of 1 and skip the multiply
    // private static final long BYTE_ARRAY_INDEX_SCALE = 1;

    private static final long           BOOLEAN_ARRAY_BASE_OFFSET = arrayBaseOffset(boolean[].class);
    private static final long           BOOLEAN_ARRAY_INDEX_SCALE = arrayIndexScale(boolean[].class);

    private static final long           INT_ARRAY_BASE_OFFSET     = arrayBaseOffset(int[].class);
    private static final long           INT_ARRAY_INDEX_SCALE     = arrayIndexScale(int[].class);

    private static final long           LONG_ARRAY_BASE_OFFSET    = arrayBaseOffset(long[].class);
    private static final long           LONG_ARRAY_INDEX_SCALE    = arrayIndexScale(long[].class);

    private static final long           FLOAT_ARRAY_BASE_OFFSET   = arrayBaseOffset(float[].class);
    private static final long           FLOAT_ARRAY_INDEX_SCALE   = arrayIndexScale(float[].class);

    private static final long           DOUBLE_ARRAY_BASE_OFFSET  = arrayBaseOffset(double[].class);
    private static final long           DOUBLE_ARRAY_INDEX_SCALE  = arrayIndexScale(double[].class);

    private static final long           OBJECT_ARRAY_BASE_OFFSET  = arrayBaseOffset(Object[].class);
    private static final long           OBJECT_ARRAY_INDEX_SCALE  = arrayIndexScale(Object[].class);

    private static final long           BUFFER_ADDRESS_OFFSET     = objectFieldOffset(bufferAddressField());

    private static final long           STRING_VALUE_OFFSET       = objectFieldOffset(stringValueField());

    /**
     * Whether or not can use the unsafe api.
     */
    public static boolean hasUnsafe() {
        return UNSAFE != null;
    }

    /**
     * Get a {@link UnsafeAccessor} appropriate for the platform.
     */
    public static UnsafeAccessor getUnsafeAccessor() {
        return UNSAFE_ACCESSOR;
    }

    public static byte getByte(Object target, long offset) {
        return UNSAFE_ACCESSOR.getByte(target, offset);
    }

    public static void putByte(Object target, long offset, byte value) {
        UNSAFE_ACCESSOR.putByte(target, offset, value);
    }

    public static int getInt(Object target, long offset) {
        return UNSAFE_ACCESSOR.getInt(target, offset);
    }

    public static void putInt(Object target, long offset, int value) {
        UNSAFE_ACCESSOR.putInt(target, offset, value);
    }

    public static long getLong(Object target, long offset) {
        return UNSAFE_ACCESSOR.getLong(target, offset);
    }

    public static void putLong(Object target, long offset, long value) {
        UNSAFE_ACCESSOR.putLong(target, offset, value);
    }

    public static boolean getBoolean(Object target, long offset) {
        return UNSAFE_ACCESSOR.getBoolean(target, offset);
    }

    public static void putBoolean(Object target, long offset, boolean value) {
        UNSAFE_ACCESSOR.putBoolean(target, offset, value);
    }

    public static float getFloat(Object target, long offset) {
        return UNSAFE_ACCESSOR.getFloat(target, offset);
    }

    public static void putFloat(Object target, long offset, float value) {
        UNSAFE_ACCESSOR.putFloat(target, offset, value);
    }

    public static double getDouble(Object target, long offset) {
        return UNSAFE_ACCESSOR.getDouble(target, offset);
    }

    public static void putDouble(Object target, long offset, double value) {
        UNSAFE_ACCESSOR.putDouble(target, offset, value);
    }

    public static Object getObject(Object target, long offset) {
        return UNSAFE_ACCESSOR.getObject(target, offset);
    }

    public static void putObject(Object target, long offset, Object value) {
        UNSAFE_ACCESSOR.putObject(target, offset, value);
    }

    public static byte getByte(byte[] target, long index) {
        return UNSAFE_ACCESSOR.getByte(target, BYTE_ARRAY_BASE_OFFSET + index);
    }

    public static void putByte(byte[] target, long index, byte value) {
        UNSAFE_ACCESSOR.putByte(target, BYTE_ARRAY_BASE_OFFSET + index, value);
    }

    public static int getInt(int[] target, long index) {
        return UNSAFE_ACCESSOR.getInt(target, INT_ARRAY_BASE_OFFSET + (index * INT_ARRAY_INDEX_SCALE));
    }

    public static void putInt(int[] target, long index, int value) {
        UNSAFE_ACCESSOR.putInt(target, INT_ARRAY_BASE_OFFSET + (index * INT_ARRAY_INDEX_SCALE), value);
    }

    public static long getLong(long[] target, long index) {
        return UNSAFE_ACCESSOR.getLong(target, LONG_ARRAY_BASE_OFFSET + (index * LONG_ARRAY_INDEX_SCALE));
    }

    public static void putLong(long[] target, long index, long value) {
        UNSAFE_ACCESSOR.putLong(target, LONG_ARRAY_BASE_OFFSET + (index * LONG_ARRAY_INDEX_SCALE), value);
    }

    public static boolean getBoolean(boolean[] target, long index) {
        return UNSAFE_ACCESSOR.getBoolean(target, BOOLEAN_ARRAY_BASE_OFFSET + (index * BOOLEAN_ARRAY_INDEX_SCALE));
    }

    public static void putBoolean(boolean[] target, long index, boolean value) {
        UNSAFE_ACCESSOR.putBoolean(target, BOOLEAN_ARRAY_BASE_OFFSET + (index * BOOLEAN_ARRAY_INDEX_SCALE), value);
    }

    public static float getFloat(float[] target, long index) {
        return UNSAFE_ACCESSOR.getFloat(target, FLOAT_ARRAY_BASE_OFFSET + (index * FLOAT_ARRAY_INDEX_SCALE));
    }

    public static void putFloat(float[] target, long index, float value) {
        UNSAFE_ACCESSOR.putFloat(target, FLOAT_ARRAY_BASE_OFFSET + (index * FLOAT_ARRAY_INDEX_SCALE), value);
    }

    public static double getDouble(double[] target, long index) {
        return UNSAFE_ACCESSOR.getDouble(target, DOUBLE_ARRAY_BASE_OFFSET + (index * DOUBLE_ARRAY_INDEX_SCALE));
    }

    public static void putDouble(double[] target, long index, double value) {
        UNSAFE_ACCESSOR.putDouble(target, DOUBLE_ARRAY_BASE_OFFSET + (index * DOUBLE_ARRAY_INDEX_SCALE), value);
    }

    public static Object getObject(Object[] target, long index) {
        return UNSAFE_ACCESSOR.getObject(target, OBJECT_ARRAY_BASE_OFFSET + (index * OBJECT_ARRAY_INDEX_SCALE));
    }

    public static void putObject(Object[] target, long index, Object value) {
        UNSAFE_ACCESSOR.putObject(target, OBJECT_ARRAY_BASE_OFFSET + (index * OBJECT_ARRAY_INDEX_SCALE), value);
    }

    public static byte getByte(long address) {
        return UNSAFE_ACCESSOR.getByte(address);
    }

    public static void putByte(long address, byte value) {
        UNSAFE_ACCESSOR.putByte(address, value);
    }

    public static int getInt(long address) {
        return UNSAFE_ACCESSOR.getInt(address);
    }

    public static void putInt(long address, int value) {
        UNSAFE_ACCESSOR.putInt(address, value);
    }

    public static long getLong(long address) {
        return UNSAFE_ACCESSOR.getLong(address);
    }

    public static void putLong(long address, long value) {
        UNSAFE_ACCESSOR.putLong(address, value);
    }

    public static byte getByteVolatile(byte[] target, long index) {
        return UNSAFE_ACCESSOR.getByteVolatile(target, BYTE_ARRAY_BASE_OFFSET + index);
    }

    public static void putByteVolatile(byte[] target, long index, byte value) {
        UNSAFE_ACCESSOR.putByteVolatile(target, BYTE_ARRAY_BASE_OFFSET + index, value);
    }

    public static int getIntVolatile(int[] target, long index) {
        return UNSAFE_ACCESSOR.getIntVolatile(target, INT_ARRAY_BASE_OFFSET + (index * INT_ARRAY_INDEX_SCALE));
    }

    public static void putIntVolatile(int[] target, long index, int value) {
        UNSAFE_ACCESSOR.putIntVolatile(target, INT_ARRAY_BASE_OFFSET + (index * INT_ARRAY_INDEX_SCALE), value);
    }

    public static long getLongVolatile(long[] target, long index) {
        return UNSAFE_ACCESSOR.getLongVolatile(target, LONG_ARRAY_BASE_OFFSET + (index * LONG_ARRAY_INDEX_SCALE));
    }

    public static void putLongVolatile(long[] target, long index, long value) {
        UNSAFE_ACCESSOR.putLongVolatile(target, LONG_ARRAY_BASE_OFFSET + (index * LONG_ARRAY_INDEX_SCALE), value);
    }

    public static boolean getBooleanVolatile(boolean[] target, long index) {
        return UNSAFE_ACCESSOR.getBooleanVolatile(target, BOOLEAN_ARRAY_BASE_OFFSET
                + (index * BOOLEAN_ARRAY_INDEX_SCALE));
    }

    public static void putBooleanVolatile(boolean[] target, long index, boolean value) {
        UNSAFE_ACCESSOR.putBooleanVolatile(target, BOOLEAN_ARRAY_BASE_OFFSET + (index * BOOLEAN_ARRAY_INDEX_SCALE),
                value);
    }

    public static float getFloatVolatile(float[] target, long index) {
        return UNSAFE_ACCESSOR.getFloatVolatile(target, FLOAT_ARRAY_BASE_OFFSET + (index * FLOAT_ARRAY_INDEX_SCALE));
    }

    public static void putFloatVolatile(float[] target, long index, float value) {
        UNSAFE_ACCESSOR.putFloatVolatile(target, FLOAT_ARRAY_BASE_OFFSET + (index * FLOAT_ARRAY_INDEX_SCALE), value);
    }

    public static double getDoubleVolatile(double[] target, long index) {
        return UNSAFE_ACCESSOR.getDoubleVolatile(target, DOUBLE_ARRAY_BASE_OFFSET + (index * DOUBLE_ARRAY_INDEX_SCALE));
    }

    public static void putDoubleVolatile(double[] target, long index, double value) {
        UNSAFE_ACCESSOR.putDoubleVolatile(target, DOUBLE_ARRAY_BASE_OFFSET + (index * DOUBLE_ARRAY_INDEX_SCALE), value);
    }

    public static Object getObjectVolatile(Object[] target, long index) {
        return UNSAFE_ACCESSOR.getObjectVolatile(target, OBJECT_ARRAY_BASE_OFFSET + (index * OBJECT_ARRAY_INDEX_SCALE));
    }

    public static void putObjectVolatile(Object[] target, long index, Object value) {
        UNSAFE_ACCESSOR.putObjectVolatile(target, OBJECT_ARRAY_BASE_OFFSET + (index * OBJECT_ARRAY_INDEX_SCALE), value);
    }

    /**
     * Reports the offset of the first element in the storage allocation of a
     * given array class.
     */
    public static int arrayBaseOffset(Class<?> clazz) {
        return hasUnsafe() ? UNSAFE_ACCESSOR.arrayBaseOffset(clazz) : -1;
    }

    /**
     * Reports the scale factor for addressing elements in the storage
     * allocation of a given array class.
     */
    public static int arrayIndexScale(Class<?> clazz) {
        return hasUnsafe() ? UNSAFE_ACCESSOR.arrayIndexScale(clazz) : -1;
    }

    /**
     * Returns the offset of the provided field, or {@code -1} if {@code sun.misc.Unsafe} is not
     * available.
     */
    public static long objectFieldOffset(Field field) {
        return field == null || hasUnsafe() ? UNSAFE_ACCESSOR.objectFieldOffset(field) : -1;
    }

    /**
     * Returns the offset of the provided class and fieldName, or {@code -1} if {@code sun.misc.Unsafe} is not
     * available.
     */
    public static long objectFieldOffset(Class<?> clazz, String fieldName) {
        try {
            return objectFieldOffset(clazz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            UNSAFE_ACCESSOR.throwException(e);
        }
        return -1; // never get here
    }

    /**
     * Gets the offset of the {@code address} field of the given
     * direct {@link ByteBuffer}.
     */
    public static long addressOffset(ByteBuffer buffer) {
        return UNSAFE_ACCESSOR.getLong(buffer, BUFFER_ADDRESS_OFFSET);
    }

    public static void throwException(Throwable t) {
        UNSAFE_ACCESSOR.throwException(t);
    }

    /**
     * Returns a new {@link String} backed by the given {@code chars}.
     * The char array should not be mutated any more after calling
     * this function.
     */
    public static String moveToString(char[] chars) {
        if (STRING_VALUE_OFFSET == -1) {
            // In the off-chance that this JDK does not implement String as we'd expect, just do a copy.
            return new String(chars);
        }
        String str;
        try {
            str = (String) UNSAFE_ACCESSOR.allocateInstance(String.class);
        } catch (InstantiationException e) {
            // This should never happen, but return a copy as a fallback just in case.
            return new String(chars);
        }
        UNSAFE_ACCESSOR.putObject(str, STRING_VALUE_OFFSET, chars);
        return str;
    }

    /**
     * Returns the system {@link ClassLoader}.
     */
    public static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) ClassLoader::getSystemClassLoader);
        }
    }

    /**
     * Finds the address field within a direct {@link Buffer}.
     */
    private static Field bufferAddressField() {
        return field(Buffer.class, "address", long.class);
    }

    /**
     * Finds the value field within a {@link String}.
     */
    private static Field stringValueField() {
        return field(String.class, "value", char[].class);
    }

    /**
     * Gets the field with the given name within the class, or
     * {@code null} if not found. If found, the field is made accessible.
     */
    private static Field field(Class<?> clazz, String fieldName, Class<?> expectedType) {
        Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (!field.getType().equals(expectedType)) {
                return null;
            }
        } catch (Throwable t) {
            // Failed to access the fields.
            field = null;
        }
        return field;
    }

    private static UnsafeAccessor getUnsafeAccessor0() {
        return hasUnsafe() ? new UnsafeAccessor(UNSAFE) : null;
    }

    private static Object getUnsafe0() {
        Object unsafe;
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = unsafeField.get(null);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("sun.misc.Unsafe.theUnsafe: unavailable, {}.", t);
            }
            unsafe = null;
        }
        return unsafe;
    }

    public static class UnsafeAccessor {

        private final sun.misc.Unsafe unsafe;

        UnsafeAccessor(Object unsafe) {
            this.unsafe = (sun.misc.Unsafe) unsafe;
        }

        /**
         * Returns the {@link sun.misc.Unsafe}'s instance.
         */
        public sun.misc.Unsafe getUnsafe() {
            return unsafe;
        }

        public byte getByte(Object target, long offset) {
            return this.unsafe.getByte(target, offset);
        }

        public void putByte(Object target, long offset, byte value) {
            this.unsafe.putByte(target, offset, value);
        }

        public short getShort(Object target, long offset) {
            return this.unsafe.getShort(target, offset);
        }

        public void putShort(Object target, long offset, short value) {
            this.unsafe.putShort(target, offset, value);
        }

        public int getInt(Object target, long offset) {
            return this.unsafe.getInt(target, offset);
        }

        public void putInt(Object target, long offset, int value) {
            this.unsafe.putInt(target, offset, value);
        }

        public long getLong(Object target, long offset) {
            return this.unsafe.getLong(target, offset);
        }

        public void putLong(Object target, long offset, long value) {
            this.unsafe.putLong(target, offset, value);
        }

        public boolean getBoolean(Object target, long offset) {
            return this.unsafe.getBoolean(target, offset);
        }

        public void putBoolean(Object target, long offset, boolean value) {
            this.unsafe.putBoolean(target, offset, value);
        }

        public float getFloat(Object target, long offset) {
            return this.unsafe.getFloat(target, offset);
        }

        public void putFloat(Object target, long offset, float value) {
            this.unsafe.putFloat(target, offset, value);
        }

        public double getDouble(Object target, long offset) {
            return this.unsafe.getDouble(target, offset);
        }

        public void putDouble(Object target, long offset, double value) {
            this.unsafe.putDouble(target, offset, value);
        }

        public Object getObject(Object target, long offset) {
            return this.unsafe.getObject(target, offset);
        }

        public void putObject(Object target, long offset, Object value) {
            this.unsafe.putObject(target, offset, value);
        }

        public byte getByte(long address) {
            return this.unsafe.getByte(address);
        }

        public void putByte(long address, byte value) {
            this.unsafe.putByte(address, value);
        }

        public short getShort(long address) {
            return this.unsafe.getShort(address);
        }

        public void putShort(long address, short value) {
            this.unsafe.putShort(address, value);
        }

        public int getInt(long address) {
            return this.unsafe.getInt(address);
        }

        public void putInt(long address, int value) {
            this.unsafe.putInt(address, value);
        }

        public long getLong(long address) {
            return this.unsafe.getLong(address);
        }

        public void putLong(long address, long value) {
            this.unsafe.putLong(address, value);
        }

        public void copyMemory(Object srcBase, long srcOffset, Object dstBase, long dstOffset, long bytes) {
            this.unsafe.copyMemory(srcBase, srcOffset, dstBase, dstOffset, bytes);
        }

        public void copyMemory(long srcAddress, long dstAddress, long bytes) {
            this.unsafe.copyMemory(srcAddress, dstAddress, bytes);
        }

        public byte getByteVolatile(Object target, long offset) {
            return this.unsafe.getByteVolatile(target, offset);
        }

        public void putByteVolatile(Object target, long offset, byte value) {
            this.unsafe.putByteVolatile(target, offset, value);
        }

        public short getShortVolatile(Object target, long offset) {
            return this.unsafe.getShortVolatile(target, offset);
        }

        public void putShortVolatile(Object target, long offset, short value) {
            this.unsafe.putShortVolatile(target, offset, value);
        }

        public int getIntVolatile(Object target, long offset) {
            return this.unsafe.getIntVolatile(target, offset);
        }

        public void putIntVolatile(Object target, long offset, int value) {
            this.unsafe.putIntVolatile(target, offset, value);
        }

        public long getLongVolatile(Object target, long offset) {
            return this.unsafe.getLongVolatile(target, offset);
        }

        public void putLongVolatile(Object target, long offset, long value) {
            this.unsafe.putLongVolatile(target, offset, value);
        }

        public boolean getBooleanVolatile(Object target, long offset) {
            return this.unsafe.getBooleanVolatile(target, offset);
        }

        public void putBooleanVolatile(Object target, long offset, boolean value) {
            this.unsafe.putBooleanVolatile(target, offset, value);
        }

        public float getFloatVolatile(Object target, long offset) {
            return this.unsafe.getFloatVolatile(target, offset);
        }

        public void putFloatVolatile(Object target, long offset, float value) {
            this.unsafe.putFloatVolatile(target, offset, value);
        }

        public double getDoubleVolatile(Object target, long offset) {
            return this.unsafe.getDoubleVolatile(target, offset);
        }

        public void putDoubleVolatile(Object target, long offset, double value) {
            this.unsafe.putDoubleVolatile(target, offset, value);
        }

        public Object getObjectVolatile(Object target, long offset) {
            return this.unsafe.getObjectVolatile(target, offset);
        }

        public void putObjectVolatile(Object target, long offset, Object value) {
            this.unsafe.putObjectVolatile(target, offset, value);
        }

        /**
         * Reports the offset of the first element in the storage allocation of a
         * given array class.
         */
        public int arrayBaseOffset(Class<?> clazz) {
            return this.unsafe != null ? this.unsafe.arrayBaseOffset(clazz) : -1;
        }

        /**
         * Reports the scale factor for addressing elements in the storage
         * allocation of a given array class.
         */
        public int arrayIndexScale(Class<?> clazz) {
            return this.unsafe != null ? this.unsafe.arrayIndexScale(clazz) : -1;
        }

        /**
         * Returns the offset of the provided field, or {@code -1} if {@code sun.misc.Unsafe} is not
         * available.
         */
        public long objectFieldOffset(Field field) {
            return field == null || this.unsafe == null ? -1 : this.unsafe.objectFieldOffset(field);
        }

        public Object allocateInstance(Class<?> clazz) throws InstantiationException {
            return this.unsafe.allocateInstance(clazz);
        }

        public void throwException(Throwable t) {
            this.unsafe.throwException(t);
        }
    }

    private UnsafeUtil() {
    }
}
