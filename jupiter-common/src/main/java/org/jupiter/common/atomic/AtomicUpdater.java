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
package org.jupiter.common.atomic;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jupiter.common.util.internal.UnsafeUtil;

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
     * @param tClass    the class of the objects holding the field.
     * @param vClass    the class of the field
     * @param fieldName the name of the field to be updated.
     */
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newAtomicReferenceFieldUpdater(
            Class<U> tClass, Class<W> vClass, String fieldName) {
        try {
            return new UnsafeAtomicReferenceFieldUpdater<>(UnsafeUtil.getUnsafe(), tClass, fieldName);
        } catch (Throwable t) {
            return AtomicReferenceFieldUpdater.newUpdater(tClass, vClass, fieldName);
        }
    }

    private AtomicUpdater() {}
}
