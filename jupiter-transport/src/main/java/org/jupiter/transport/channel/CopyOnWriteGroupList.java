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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

import static org.jupiter.common.util.Preconditions.checkNotNull;

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

    private final DirectoryJChannelGroup parent;

    transient final ReentrantLock lock = new ReentrantLock();

    private volatile transient JChannelGroup[] array;
    private transient boolean sameWeight; // 无volatile修饰, 通过array保证可见性

    public CopyOnWriteGroupList(DirectoryJChannelGroup parent) {
        this.parent = parent;
        setArray(EMPTY_ARRAY);
    }

    public final JChannelGroup[] snapshot() {
        return getArray();
    }

    public final boolean isSameWeight() {
        // first read volatile
        return getArray().length == 0 || sameWeight;
    }

    public final void setSameWeight(boolean sameWeight) {
        JChannelGroup[] elements = getArray();
        setArray(elements, sameWeight); // ensures volatile write semantics
    }

    final JChannelGroup[] getArray() {
        return array;
    }

    final void setArray(JChannelGroup[] a) {
        sameWeight = false;
        array = a;
    }

    final void setArray(JChannelGroup[] a, boolean sameWeight) {
        this.sameWeight = sameWeight;
        array = a;
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

    public int indexOf(JChannelGroup e, int index) {
        JChannelGroup[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
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

    @SuppressWarnings("all")
    public boolean remove(JChannelGroup o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            JChannelGroup[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                // Copy while searching for element to remove
                // This wins in the normal case of element being present
                int newlen = len - 1;
                JChannelGroup[] newElements = new JChannelGroup[newlen];

                for (int i = 0; i < newlen; ++i) {
                    if (eq(o, elements[i])) {
                        // found one;  copy remaining and exit
                        for (int k = i + 1; k < len; ++k) {
                            newElements[k - 1] = elements[k];
                        }
                        setArray(newElements);
                        parent.decrementRefCount(o); // ref count -1
                        return true;
                    } else {
                        newElements[i] = elements[i];
                    }
                }

                // special handling for last cell
                if (eq(o, elements[newlen])) {
                    setArray(newElements);
                    parent.decrementRefCount(o); // ref count -1
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean addIfAbsent(JChannelGroup o) {
        checkNotNull(o, "group");

        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // Copy while checking if already present.
            // This wins in the most common case where it is not present
            JChannelGroup[] elements = getArray();
            int len = elements.length;
            JChannelGroup[] newElements = new JChannelGroup[len + 1];
            for (int i = 0; i < len; ++i) {
                if (eq(o, elements[i])) {
                    return false; // exit, throwing away copy
                } else {
                    newElements[i] = elements[i];
                }
            }
            newElements[len] = o;
            setArray(newElements);
            parent.incrementRefCount(o); // ref count +1
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

        for (int i = 0; i < len; ++i) {
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
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
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