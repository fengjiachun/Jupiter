package org.jupiter.common.util.internal;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

/**
 * For the {@link sun.misc.Unsafe} access.
 *
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
public class UnsafeAccess {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UnsafeAccess.class);

    public static final Unsafe UNSAFE;

    public static final boolean SUPPORTS_GET_AND_SET;

    public static final int JAVA_VERSION = javaVersion0();

    private static final long ADDRESS_FIELD_OFFSET;

    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            SUPPORTS_GET_AND_SET = false;
            throw new RuntimeException(e);
        }

        boolean getAndSetSupport = false;
        try {
            Unsafe.class.getMethod("getAndSetObject", Object.class, Long.TYPE, Object.class);
            getAndSetSupport = true;
        } catch (Throwable ignored) {}
        SUPPORTS_GET_AND_SET = getAndSetSupport;

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
