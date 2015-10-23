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
        return new ArrayList<E>();
    }

    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> newArrayList(E... elements) {
        checkNotNull(elements);
        // Avoid integer overflow when a large array is passed in
        int capacity = computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList<E>(capacity);
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
        return new ArrayList<E>(initialArraySize);
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
