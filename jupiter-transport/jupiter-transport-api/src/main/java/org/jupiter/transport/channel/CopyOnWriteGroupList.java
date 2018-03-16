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

package org.jupiter.transport.channel;

import org.jupiter.transport.Directory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 相同服务, 不同服务节点的channel group容器,
 * 线程安全(写时复制), 实现原理类似 {@link java.util.concurrent.CopyOnWriteArrayList}.
 *
 * update操作仅支持addIfAbsent/remove, update操作会同时更新对应服务节点(group)的引用计数.
 *
 * jupiter
 * org.jupiter.transport.channel
 *
 * @author jiachun.fjc
 */
public class CopyOnWriteGroupList {

    private static final JChannelGroup[] EMPTY_ARRAY = new JChannelGroup[0];

    private transient final ReentrantLock lock = new ReentrantLock();

    private final DirectoryJChannelGroup parent;
    // 0: JChannelGroup[], 1: Map<Directory, WeightArray>
    private volatile transient Object[] data;

    public CopyOnWriteGroupList(DirectoryJChannelGroup parent) {
        this.parent = parent;
        setArray(EMPTY_ARRAY);
    }

    public final JChannelGroup[] snapshot() {
        return getArray();
    }

    @SuppressWarnings("unchecked")
    public final Object weightArray(JChannelGroup[] snapshot, Directory directory) {
        Object[] array = data; // data snapshot
        return array[0] != snapshot
                ? null
                : (array[1] == null ? null : ((Map<Directory, Object>) (array[1])).get(directory));
    }

    public final boolean setWeightInfo(
            JChannelGroup[] snapshot, Directory directory, Object weightArray) {
        JChannelGroup[] elements = getArray();
        if (elements == snapshot) {
            final ReentrantLock lock = this.lock;
            boolean locked = lock.tryLock();
            if (locked) {
                try {
                    setArray(elements, directory, weightArray);
                    return true;
                } finally {
                    lock.unlock();
                }
            }
        }
        return false;
    }

    private JChannelGroup[] getArray() {
        return (JChannelGroup[]) data[0];
    }

    private void setArray(JChannelGroup[] a) {
        data = new Object[] { a, null, null };
    }

    @SuppressWarnings("unchecked")
    private void setArray(JChannelGroup[] a, Directory directory, Object weightArray) {
        Map<Directory, Object> weightsMap = (Map<Directory, Object>) data[1];

        if (weightArray != null) {
            if (weightsMap == null) {
                weightsMap = new HashMap<>();
            }
            weightsMap.put(directory, weightArray);
        }

        data = new Object[] { a, weightsMap };
    }

    public int size() {
        return getArray().length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(JChannelGroup o) {
        JChannelGroup[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    public int indexOf(JChannelGroup o) {
        JChannelGroup[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }

    public int indexOf(JChannelGroup o, int index) {
        JChannelGroup[] elements = getArray();
        return indexOf(o, elements, index, elements.length);
    }

    public JChannelGroup[] toArray() {
        JChannelGroup[] elements = getArray();
        return Arrays.copyOf(elements, elements.length);
    }

    private JChannelGroup get(JChannelGroup[] array, int index) {
        return array[index];
    }

    public JChannelGroup get(int index) {
        return get(getArray(), index);
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean remove(JChannelGroup o) {
        JChannelGroup[] snapshot = getArray();
        int index = indexOf(o, snapshot, 0, snapshot.length);
        return (index >= 0) && remove(o, snapshot, index);
    }

    /**
     * A version of remove(JChannelGroup) using the strong hint that given
     * recent snapshot contains o at the given index.
     */
    private boolean remove(JChannelGroup o, JChannelGroup[] snapshot, int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            JChannelGroup[] current = getArray();
            int len = current.length;
            if (snapshot != current) findIndex: {
                int prefix = Math.min(index, len);
                for (int i = 0; i < prefix; i++) {
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        index = i;
                        break findIndex;
                    }
                }
                if (index >= len) {
                    return false;
                }
                if (current[index] == o) {
                    break findIndex;
                }
                index = indexOf(o, current, index, len);
                if (index < 0) {
                    return false;
                }
            }
            JChannelGroup[] newElements = new JChannelGroup[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1, newElements, index, len - index - 1);
            setArray(newElements);
            parent.decrementRefCount(o); // reference count -1
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Appends the element, if not present.
     *
     * @param o element to be added to this list, if absent
     * @return {@code true} if the element was added
     */
    public boolean addIfAbsent(JChannelGroup o) {
        JChannelGroup[] snapshot = getArray();
        return indexOf(o, snapshot, 0, snapshot.length) < 0 && addIfAbsent(o, snapshot);
    }

    /**
     * A version of addIfAbsent using the strong hint that given
     * recent snapshot does not contain o.
     */
    private boolean addIfAbsent(JChannelGroup o, JChannelGroup[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            JChannelGroup[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // optimize for lost race to another addXXX operation
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++) {
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        return false;
                    }
                }
                if (indexOf(o, current, common, len) >= 0) {
                    return false;
                }
            }
            JChannelGroup[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = o;
            setArray(newElements);
            parent.incrementRefCount(o); // reference count +1
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean containsAll(Collection<? extends JChannelGroup> c) {
        JChannelGroup[] elements = getArray();
        int len = elements.length;
        for (JChannelGroup e : c) {
            if (indexOf(e, elements, 0, len) < 0) {
                return false;
            }
        }
        return true;
    }

    void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            setArray(EMPTY_ARRAY);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(getArray());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CopyOnWriteGroupList)) {
            return false;
        }

        CopyOnWriteGroupList other = (CopyOnWriteGroupList) (o);

        JChannelGroup[] elements = getArray();
        JChannelGroup[] otherElements = other.getArray();
        int len = elements.length;
        int otherLen = otherElements.length;

        if (len != otherLen) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            if (!eq(elements[i], otherElements[i])) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("all")
    @Override
    public int hashCode() {
        int hashCode = 1;
        JChannelGroup[] elements = getArray();
        for (int i = 0, len = elements.length; i < len; i++) {
            JChannelGroup o = elements[i];
            hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
        }
        return hashCode;
    }

    private boolean eq(JChannelGroup o1, JChannelGroup o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    private int indexOf(JChannelGroup o, JChannelGroup[] elements, int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = index; i < fence; i++) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
}