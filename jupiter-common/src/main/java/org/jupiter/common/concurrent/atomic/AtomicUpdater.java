package org.jupiter.common.concurrent.atomic;

import org.jupiter.common.util.internal.UnsafeAccess;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A tool utility that enables atomic updates to designated {@code volatile} fields of designated classes.
 *
 * jupiter
 * org.jupiter.common.concurrent.atomic
 *
 * @author jiachun.fjc
 */
public final class AtomicUpdater {

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass the class of the objects holding the field.
     * @param vClass the class of the field
     * @param fieldName the name of the field to be updated.
     */
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newAtomicReferenceFieldUpdater(
            Class<U> tClass, Class<W> vClass, String fieldName) {
        try {
            return new UnsafeAtomicReferenceFieldUpdater<>(UnsafeAccess.UNSAFE, tClass, fieldName);
        } catch (Throwable t) {
            return AtomicReferenceFieldUpdater.newUpdater(tClass, vClass, fieldName);
        }
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <T> AtomicIntegerFieldUpdater<T> newAtomicIntegerFieldUpdater(Class<T> tClass, String fieldName) {
        try {
            return new UnsafeAtomicIntegerFieldUpdater<>(UnsafeAccess.UNSAFE, tClass, fieldName);
        } catch (Throwable t) {
            return AtomicIntegerFieldUpdater.newUpdater(tClass, fieldName);
        }
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <T> AtomicLongFieldUpdater<T> newAtomicLongFieldUpdater(Class<T> tClass, String fieldName) {
        try {
            return new UnsafeAtomicLongFieldUpdater<>(UnsafeAccess.UNSAFE, tClass, fieldName);
        } catch (Throwable t) {
            return AtomicLongFieldUpdater.newUpdater(tClass, fieldName);
        }
    }

    private AtomicUpdater() {}
}
