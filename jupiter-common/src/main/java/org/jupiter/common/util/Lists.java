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

package org.jupiter.common.util;

import org.jupiter.common.util.internal.RecyclableArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class Lists {

    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> newArrayList(E... elements) {
        checkNotNull(elements);
        // Avoid integer overflow when a large array is passed in
        int capacity = computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList<>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
        checkNotNull(elements);
        return elements instanceof Collection ? new ArrayList((Collection<E>) elements) : newArrayList(elements.iterator());
    }

    public static <E> ArrayList<E> newArrayListWithCapacity(int initialArraySize) {
        checkArgument(initialArraySize >= 0);
        return new ArrayList<>(initialArraySize);
    }

    /**
     * Returns a simple list which is recyclable.
     * <p/>
     * <pre>
     *     List&lt{@link Object}&gt list = Lists.newRecyclableArrayList();
     *     try {
     *         // ...
     *     } finally {
     *         Lists.recycleArrayList(list);
     *     }
     * </pre>
     */
    public static RecyclableArrayList newRecyclableArrayList() {
        return RecyclableArrayList.newInstance();
    }

    /**
     * Recycle the RecyclableArrayList.
     * <p/>
     * <pre>
     *     List&lt{@link Object}&gt list = Lists.newRecyclableArrayList();
     *     try {
     *         // ...
     *     } finally {
     *         Lists.recycleArrayList(list);
     *     }
     * </pre>
     */
    public static boolean recycleArrayList(RecyclableArrayList list) {
        return RecycleUtil.recycle(list);
    }

    static int computeArrayListCapacity(int arraySize) {
        checkArgument(arraySize >= 0);

        return Ints.saturatedCast(5L + arraySize + (arraySize / 10));
    }

    private Lists() {}
}
