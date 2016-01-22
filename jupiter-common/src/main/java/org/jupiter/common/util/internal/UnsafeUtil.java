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

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * For the {@link sun.misc.Unsafe} access.
 *
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
public class UnsafeUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UnsafeUtil.class);

    public static final Unsafe UNSAFE;

    public static final boolean SUPPORTS_GET_AND_SET;
    public static final int JAVA_VERSION = javaVersion0();
    public static final long CWL_ARRAY_OFFSET; // CopyOnWriteArrayList's array offset
    public static final long STRING_BUILDER_VALUE_OFFSET;

    private static final long ADDRESS_FIELD_OFFSET;

    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Throwable t) {
            throw new UnsupportedOperationException("Unsafe", t);
        }

        boolean getAndSetSupport = false;
        try {
            Unsafe.class.getMethod("getAndSetObject", Object.class, Long.TYPE, Object.class);
            getAndSetSupport = true;
        } catch (Throwable ignored) {}
        SUPPORTS_GET_AND_SET = getAndSetSupport;

        try {
            Field field = java.util.concurrent.CopyOnWriteArrayList.class.getDeclaredField("array");
            CWL_ARRAY_OFFSET = UNSAFE.objectFieldOffset(field);
        } catch (Throwable t) {
            throw new UnsupportedOperationException(t);
        }

        try {
            Field field = null;
            Class<?> cls = StringBuilder.class;
            while (cls != null) {
                try {
                    field = cls.getDeclaredField("value");
                } catch (Throwable ignored) {}

                cls = cls.getSuperclass();
            }
            STRING_BUILDER_VALUE_OFFSET = UNSAFE.objectFieldOffset(field);
        } catch (Throwable t) {
            throw new UnsupportedOperationException(t);
        }

        ByteBuffer direct = ByteBuffer.allocateDirect(1);
        Field addressField;
        try {
            addressField = Buffer.class.getDeclaredField("address");
            addressField.setAccessible(true);
            if (addressField.getLong(ByteBuffer.allocate(1)) != 0) {
                // A heap buffer must have 0 address.
                addressField = null;
            } else {
                if (addressField.getLong(direct) == 0) {
                    // A direct buffer must have non-zero address.
                    addressField = null;
                }
            }
        } catch (Throwable e) {
            // Failed to access the address field.
            addressField = null;
        }
        if (addressField == null) {
            ADDRESS_FIELD_OFFSET = -1;
        } else {
            ADDRESS_FIELD_OFFSET = UNSAFE.objectFieldOffset(addressField);
        }
    }

    public static long directBufferAddress(ByteBuffer buffer) {
        return UNSAFE.getLong(buffer, ADDRESS_FIELD_OFFSET);
    }

    @SuppressWarnings("all")
    private static int javaVersion0() {
        int javaVersion;

        for (;;) {
            try {
                Class.forName("java.time.Clock", false, Object.class.getClassLoader());
                javaVersion = 8;
                break;
            } catch (Exception ignored) {}

            try {
                Class.forName(
                        "java.util.concurrent.LinkedTransferQueue",
                        false,
                        java.util.concurrent.BlockingQueue.class.getClassLoader());
                javaVersion = 7;
                break;
            } catch (Exception ignored) {}

            javaVersion = 6;
            break;
        }

        logger.debug("Java version: {}.", javaVersion);

        return javaVersion;
    }
}
