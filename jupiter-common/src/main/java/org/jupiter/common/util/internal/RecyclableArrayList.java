package org.jupiter.common.util.internal;

import org.jupiter.common.util.Recyclable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

/**
 * A simple list which is recyclable. This implementation does not allow {@code null} elements to be added.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
public final class RecyclableArrayList extends ArrayList<Object> implements Recyclable {

    private static final long serialVersionUID = -8605125654176467947L;

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    /**
     * Create a new empty {@link RecyclableArrayList} instance
     */
    public static RecyclableArrayList newInstance() {
        return newInstance(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Create a new empty {@link RecyclableArrayList} instance with the given capacity.
     */
    public static RecyclableArrayList newInstance(int minCapacity) {
        RecyclableArrayList ret = recyclers.get();
        ret.ensureCapacity(minCapacity);
        return ret;
    }

    @Override
    public boolean addAll(Collection<?> c) {
        checkNullElements(c);
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        checkNullElements(c);
        return super.addAll(index, c);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void checkNullElements(Collection<?> c) {
        if (c instanceof RandomAccess && c instanceof List) {
            // produce less garbage
            List<?> list = (List<?>) c;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                if (list.get(i) == null) {
                    throw new IllegalArgumentException("c contains null values");
                }
            }
        } else {
            for (Object element : c) {
                if (element == null) {
                    throw new IllegalArgumentException("c contains null values");
                }
            }
        }
    }

    @Override
    public boolean add(Object element) {
        if (element == null) {
            throw new NullPointerException("element");
        }
        return super.add(element);
    }

    @Override
    public void add(int index, Object element) {
        if (element == null) {
            throw new NullPointerException("element");
        }
        super.add(index, element);
    }

    @Override
    public Object set(int index, Object element) {
        if (element == null) {
            throw new NullPointerException("element");
        }
        return super.set(index, element);
    }

    @Override
    public boolean recycle() {
        clear();
        return recyclers.recycle(this, handle);
    }

    private RecyclableArrayList(Recyclers.Handle<RecyclableArrayList> handle) {
        this(handle, DEFAULT_INITIAL_CAPACITY);
    }

    private RecyclableArrayList(Recyclers.Handle<RecyclableArrayList> handle, int initialCapacity) {
        super(initialCapacity);
        this.handle = handle;
    }

    private transient final Recyclers.Handle<RecyclableArrayList> handle;

    private static final Recyclers<RecyclableArrayList> recyclers = new Recyclers<RecyclableArrayList>() {

        @Override
        protected RecyclableArrayList newObject(Handle<RecyclableArrayList> handle) {
            return new RecyclableArrayList(handle);
        }
    };
}
