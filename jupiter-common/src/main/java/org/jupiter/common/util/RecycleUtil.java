package org.jupiter.common.util;

/**
 * Recycle tool for {@link Recyclable}.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class RecycleUtil {

    /**
     * Recycle designated instance.
     */
    public static boolean recycle(Object obj) {
        return obj != null && obj instanceof Recyclable && ((Recyclable) obj).recycle();
    }

    private RecycleUtil() {}
}
