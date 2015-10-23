package org.jupiter.common.util;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class RecycleUtil {

    /**
     * Recycle this instance.
     */
    public static boolean recycle(Object obj) {
        return obj != null && obj instanceof Recyclable && ((Recyclable) obj).recycle();
    }

    private RecycleUtil() {}
}
