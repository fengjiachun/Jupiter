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

    public static final long CWL_ARRAY_FIELD_OFFSET;
    public static final long STRING_BUILDER_VALUE_FIELD_OFFSET;

    private static final long ADDRESS_FIELD_OFFSET;

    static {
        Field address_field;
        try {
            address_field = Buffer.class.getDeclaredField("address");
            address_field.setAccessible(true);
            if (address_field.getLong(ByteBuffer.allocate(1)) != 0) {
                // A heap buffer must have 0 address.
                address_field = null;
            } else {
                if (address_field.getLong(ByteBuffer.allocateDirect(1)) == 0) {
                    // A direct buffer must have non-zero address.
                    address_field = null;
                }
            }
        } catch (Throwable e) {
            // Failed to access the address field.
            address_field = null;
        }

        Field array_field;
        try {
            array_field = CopyOnWriteArrayList.class.getDeclaredField("array");
        } catch (Throwable t) {
            array_field = null;
        }

        Field value_field;
        try {
            value_field = StringBuilder.class.getSuperclass().getDeclaredField("value");
        } catch (Throwable t) {
            value_field = null;
        }

        Unsafe unsafe;
        if (address_field == null || array_field == null || value_field == null) {
            // If we cannot access address_field/array_field/value_field, there's no point of using unsafe.
            // Let's just pretend unsafe is unavailable for overall simplicity.
            unsafe = null;
        } else {
            try {
                Field unsafe_field = Unsafe.class.getDeclaredField("theUnsafe");
                unsafe_field.setAccessible(true);
                unsafe = (Unsafe) unsafe_field.get(null);
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
            ADDRESS_FIELD_OFFSET = UNSAFE.objectFieldOffset(address_field);
            CWL_ARRAY_FIELD_OFFSET = UNSAFE.objectFieldOffset(array_field);
            STRING_BUILDER_VALUE_FIELD_OFFSET = UNSAFE.objectFieldOffset(value_field);
        }
    }

    public static long directBufferAddress(ByteBuffer buffer) {
        return UNSAFE.getLong(buffer, ADDRESS_FIELD_OFFSET);
    }

    private UnsafeUtil() {}
}
