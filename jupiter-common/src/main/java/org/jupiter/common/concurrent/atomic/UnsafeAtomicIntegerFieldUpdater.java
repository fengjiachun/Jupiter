package org.jupiter.common.concurrent.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * jupiter
 * org.jupiter.common.concurrent.atomic
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
final class UnsafeAtomicIntegerFieldUpdater<U> extends AtomicIntegerFieldUpdater<U> {
    private final long offset;
    private final Unsafe unsafe;

    UnsafeAtomicIntegerFieldUpdater(Unsafe unsafe, Class<?> tClass, String fieldName) throws NoSuchFieldException {
        Field field = tClass.getDeclaredField(fieldName);
        if (!Modifier.isVolatile(field.getModifiers())) {
            throw new IllegalArgumentException("must be volatile");
        }
        this.unsafe = unsafe;
        offset = unsafe.objectFieldOffset(field);
    }

    @Override
    public boolean compareAndSet(U obj, int expect, int update) {
        return unsafe.compareAndSwapInt(obj, offset, expect, update);
    }

    @Override
    public boolean weakCompareAndSet(U obj, int expect, int update) {
        return unsafe.compareAndSwapInt(obj, offset, expect, update);
    }

    @Override
    public void set(U obj, int newValue) {
        unsafe.putIntVolatile(obj, offset, newValue);
    }

    @Override
    public void lazySet(U obj, int newValue) {
        unsafe.putOrderedInt(obj, offset, newValue);
    }

    @Override
    public int get(U obj) {
        return unsafe.getIntVolatile(obj, offset);
    }
}
