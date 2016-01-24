package org.jupiter.common.util.internal;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public final class UnsafeReferenceFieldUpdater<U, W> {
    private final long offset;
    private final Unsafe unsafe;

    UnsafeReferenceFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        Field field = tClass.getDeclaredField(fieldName);
        if (unsafe == null) {
            throw new NullPointerException("unsafe");
        }
        this.unsafe = unsafe;
        offset = unsafe.objectFieldOffset(field);
    }

    public boolean compareAndSet(U obj, W expect, W update) {
        return unsafe.compareAndSwapObject(obj, offset, expect, update);
    }

    public boolean weakCompareAndSet(U obj, W expect, W update) {
        return unsafe.compareAndSwapObject(obj, offset, expect, update);
    }

    public void set(U obj, W newValue) {
        unsafe.putObject(obj, offset, newValue);
    }

    public W get(U obj) {
        return (W) unsafe.getObject(obj, offset);
    }
}
