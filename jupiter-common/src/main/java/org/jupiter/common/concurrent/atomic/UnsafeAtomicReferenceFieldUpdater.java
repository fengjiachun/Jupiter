package org.jupiter.common.concurrent.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * jupiter
 * org.jupiter.common.concurrent.atomic
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
@SuppressWarnings("all")
final class UnsafeAtomicReferenceFieldUpdater<U, W> extends AtomicReferenceFieldUpdater<U, W> {
    private final long offset;
    private final Unsafe unsafe;

    UnsafeAtomicReferenceFieldUpdater(Unsafe unsafe, Class<U> tClass, String fieldName) throws NoSuchFieldException {
        Field field = tClass.getDeclaredField(fieldName);
        if (!Modifier.isVolatile(field.getModifiers())) {
            throw new IllegalArgumentException("must be volatile");
        }
        this.unsafe = unsafe;
        offset = unsafe.objectFieldOffset(field);
    }

    @Override
    public boolean compareAndSet(U obj, W expect, W update) {
        return unsafe.compareAndSwapObject(obj, offset, expect, update);
    }

    @Override
    public boolean weakCompareAndSet(U obj, W expect, W update) {
        return unsafe.compareAndSwapObject(obj, offset, expect, update);
    }

    @Override
    public void set(U obj, W newValue) {
        unsafe.putObjectVolatile(obj, offset, newValue);
    }

    @Override
    public void lazySet(U obj, W newValue) {
        unsafe.putOrderedObject(obj, offset, newValue);
    }

    @Override
    public W get(U obj) {
        return (W) unsafe.getObjectVolatile(obj, offset);
    }
}
