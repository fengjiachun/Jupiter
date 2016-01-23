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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

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

    public static final int JAVA_VERSION = javaVersion0();
    public static final long CWL_ARRAY_FIELD_OFFSET; // CopyOnWriteArrayList's array offset
    public static final long STRING_BUILDER_VALUE_FIELD_OFFSET;

    private static final long ADDRESS_FIELD_OFFSET;

    static {
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

        Field arrayField;
        try {
            arrayField = CopyOnWriteArrayList.class.getDeclaredField("array");
        } catch (Throwable t) {
            arrayField = null;
        }

        Field valueField = null;
        Class<?> cls = StringBuilder.class;
        while (cls != null) {
            try {
                valueField = cls.getDeclaredField("value");
                break;
            } catch (Throwable t) {
                cls = cls.getSuperclass();
            }
        }

        Unsafe unsafe;
        if (addressField == null || arrayField == null || valueField == null) {
            // If we cannot access addressField/arrayField/valueField, there's no point of using unsafe.
            // Let's just pretend unsafe is unavailable for overall simplicity.
            unsafe = null;
        } else {
            try {
                Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                unsafe = (Unsafe) unsafeField.get(null);
            } catch (Throwable t) {
                unsafe = null;
            }
        }

        UNSAFE = unsafe;

        if (unsafe == null) {
            ADDRESS_FIELD_OFFSET = -1;
            CWL_ARRAY_FIELD_OFFSET = -1;
            STRING_BUILDER_VALUE_FIELD_OFFSET = -1;

            logger.warn("sun.misc.Unsafe.theUnsafe: unavailable");
        } else {
            ADDRESS_FIELD_OFFSET = UNSAFE.objectFieldOffset(addressField);
            CWL_ARRAY_FIELD_OFFSET = UNSAFE.objectFieldOffset(arrayField);
            STRING_BUILDER_VALUE_FIELD_OFFSET = UNSAFE.objectFieldOffset(valueField);
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
                Class.forName("java.util.concurrent.LinkedTransferQueue", false, BlockingQueue.class.getClassLoader());
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
