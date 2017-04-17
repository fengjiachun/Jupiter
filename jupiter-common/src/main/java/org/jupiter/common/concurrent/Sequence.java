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

package org.jupiter.common.concurrent;

import org.jupiter.common.util.internal.JUnsafe;
import sun.misc.Unsafe;

/**
 * 利用对象继承的内存布局规则来padding避免false sharing, 注意其中对象头会至少占用8个字节
 * ---------------------------------------
 *  For 32 bit JVM:
 *      _mark   : 4 byte constant
 *      _klass  : 4 byte pointer to class
 *  For 64 bit JVM:
 *      _mark   : 8 byte constant
 *      _klass  : 8 byte pointer to class
 *  For 64 bit JVM with compressed-oops:
 *      _mark   : 8 byte constant
 *      _klass  : 4 byte pointer to class
 * ---------------------------------------
 */
class LhsPadding {
    @SuppressWarnings("unused")
    protected long p01, p02, p03, p04, p05, p06, p07;
}

class Value extends LhsPadding {
    protected volatile long value;
}

class RhsPadding extends Value {
    @SuppressWarnings("unused")
    protected long p09, p10, p11, p12, p13, p14, p15;
}

/**
 * Jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public class Sequence extends RhsPadding {

    private static final Unsafe UNSAFE;
    private static final long VALUE_OFFSET;

    static {
        UNSAFE = JUnsafe.getUnsafe();
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(Value.class.getDeclaredField("value"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a sequence initialised to -1.
     */
    public Sequence() {
        this(0);
    }

    /**
     * Creates a new Sequence with the given initial value.
     *
     * @param initialValue the initial value
     */
    public Sequence(long initialValue) {
        value = initialValue;
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    public final long get() {
        return value;
    }

    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    public final void set(long newValue) {
        value = newValue;
    }

    /**
     * Eventually sets to the given value.
     *
     * @param newValue the new value
     */
    public final void lazySet(long newValue) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, newValue);
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(long expect, long update) {
        return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, expect, update);
    }

    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    public long incrementAndGet() {
        return addAndGet(1L);
    }

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    public long addAndGet(long delta) {
        long currentValue;
        long newValue;

        do {
            currentValue = get();
            newValue = currentValue + delta;
        } while (!compareAndSet(currentValue, newValue));

        return newValue;
    }

    @Override
    public String toString() {
        return Long.toString(get());
    }
}
