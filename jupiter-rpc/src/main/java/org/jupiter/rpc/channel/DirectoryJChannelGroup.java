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

package org.jupiter.rpc.channel;

import org.jupiter.common.util.Maps;
import org.jupiter.rpc.Directory;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * jupiter
 * org.jupiter.rpc.channel
 *
 * @author jiachun.fjc
 */
public class DirectoryJChannelGroup {

    private static final ConcurrentMap<String, CopyOnWriteGroupList> groups = Maps.newConcurrentHashMap();
    private static final GroupRefCounterMap groupRefCounter = new GroupRefCounterMap();

    public static CopyOnWriteGroupList find(Directory directory) {
        String _directory = directory.directory();

        CopyOnWriteGroupList groupList = groups.get(_directory);
        if (groupList == null) {
            CopyOnWriteGroupList newGroupList = new CopyOnWriteGroupList();
            groupList = groups.putIfAbsent(_directory, newGroupList);
            if (groupList == null) {
                groupList = newGroupList;
            }
        }

        return groupList;
    }

    public static int getGroupRefCount(JChannelGroup group) {
        AtomicInteger counter = groupRefCounter.get(group);
        if (counter == null) {
            return 0;
        }
        return counter.get();
    }

    public static int incrementRefCount(JChannelGroup group) {
        return groupRefCounter.getOrCreate(group).incrementAndGet();
    }

    public static int decrementRefCount(JChannelGroup group) {
        AtomicInteger counter = groupRefCounter.get(group);
        if (counter == null) {
            return 0;
        }
        int count = counter.decrementAndGet();
        if (count == 0) {
            // get与remove并不是原子操作, 但在当前场景是可接受的
            groupRefCounter.remove(group);
        }
        return count;
    }

    static class GroupRefCounterMap extends ConcurrentHashMap<JChannelGroup, AtomicInteger> {

        private static final long serialVersionUID = 6590976614405397299L;

        public AtomicInteger getOrCreate(JChannelGroup key) {
            AtomicInteger counter = super.get(key);
            if (counter == null) {
                AtomicInteger newCounter = new AtomicInteger(0);
                counter = super.putIfAbsent(key, newCounter);
                if (counter == null) {
                    counter = newCounter;
                }
            }
            return counter;
        }
    }

    @SuppressWarnings("all")
    public static class CopyOnWriteGroupList {

        transient final ReentrantLock lock = new ReentrantLock();

        private volatile transient Object[] array;

        final Object[] getArray() {
            return array;
        }

        final void setArray(Object[] a) {
            array = a;
        }

        public CopyOnWriteGroupList() {
            setArray(new Object[0]);
        }

        public CopyOnWriteGroupList(Collection<? extends JChannelGroup> c) {
            Object[] elements = c.toArray();
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elements.getClass() != Object[].class) {
                elements = Arrays.copyOf(elements, elements.length, Object[].class);
            }
            setArray(elements);
        }

        public CopyOnWriteGroupList(JChannelGroup[] toCopyIn) {
            setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
        }

        public int size() {
            return getArray().length;
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public boolean contains(Object o) {
            Object[] elements = getArray();
            return indexOf(o, elements, 0, elements.length) >= 0;
        }

        public int indexOf(Object o) {
            Object[] elements = getArray();
            return indexOf(o, elements, 0, elements.length);
        }

        public int indexOf(JChannelGroup e, int index) {
            Object[] elements = getArray();
            return indexOf(e, elements, index, elements.length);
        }

        public Object[] toArray() {
            Object[] elements = getArray();
            return Arrays.copyOf(elements, elements.length);
        }

        public <T> T[] toArray(T[] a) {
            Object[] elements = getArray();
            int len = elements.length;
            if (a.length < len)
                return (T[]) Arrays.copyOf(elements, len, a.getClass());
            else {
                System.arraycopy(elements, 0, a, 0, len);
                if (a.length > len) {
                    a[len] = null;
                }
                return a;
            }
        }

        // Positional Access Operations

        private JChannelGroup get(Object[] a, int index) {
            return (JChannelGroup) a[index];
        }

        public JChannelGroup get(int index) {
            return get(getArray(), index);
        }

        public boolean remove(Object o) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                Object[] elements = getArray();
                int len = elements.length;
                if (len != 0) {
                    // Copy while searching for element to remove
                    // This wins in the normal case of element being present
                    int newlen = len - 1;
                    Object[] newElements = new Object[newlen];

                    for (int i = 0; i < newlen; ++i) {
                        if (eq(o, elements[i])) {
                            // found one;  copy remaining and exit
                            for (int k = i + 1; k < len; ++k) {
                                newElements[k - 1] = elements[k];
                            }
                            setArray(newElements);

                            // ref count -1
                            decrementRefCount((JChannelGroup) o);

                            return true;
                        } else {
                            newElements[i] = elements[i];
                        }
                    }

                    // special handling for last cell
                    if (eq(o, elements[newlen])) {
                        setArray(newElements);

                        // ref count -1
                        decrementRefCount((JChannelGroup) o);

                        return true;
                    }
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        public boolean addIfAbsent(JChannelGroup e) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                // Copy while checking if already present.
                // This wins in the most common case where it is not present
                Object[] elements = getArray();
                int len = elements.length;
                Object[] newElements = new Object[len + 1];
                for (int i = 0; i < len; ++i) {
                    if (eq(e, elements[i])) {
                        return false; // exit, throwing away copy
                    } else {
                        newElements[i] = elements[i];
                    }
                }
                newElements[len] = e;
                setArray(newElements);

                // ref count +1
                incrementRefCount(e);

                return true;
            } finally {
                lock.unlock();
            }
        }

        public boolean containsAll(Collection<?> c) {
            Object[] elements = getArray();
            int len = elements.length;
            for (Object e : c) {
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
                setArray(new Object[0]);
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

            Object[] elements = getArray();
            Object[] otherElements = other.getArray();
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

        @Override
        public int hashCode() {
            int hashCode = 1;
            Object[] elements = getArray();
            int len = elements.length;
            for (int i = 0; i < len; ++i) {
                Object obj = elements[i];
                hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
            }
            return hashCode;
        }

        private static boolean eq(Object o1, Object o2) {
            return (o1 == null ? o2 == null : o1.equals(o2));
        }

        private static int indexOf(Object o, Object[] elements, int index, int fence) {
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
}
