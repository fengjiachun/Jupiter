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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * jupiter
 * org.jupiter.rpc.channel
 *
 * @author jiachun.fjc
 */
public class DirectoryJChannelGroup {

    private static final ConcurrentMap<String, CopyOnWriteGroupList> groups = Maps.newConcurrentHashMap();
    private static final GroupRefCounterMap groupRefCounter = new GroupRefCounterMap();

    public static CopyOnWriteGroupList list(Directory directory) {
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

    public static CopyOnWriteGroupList remove(Directory directory) {
        // 一台机器的所有服务全部下线才会走到这里, 并发问题(get与remove并不是原子操作)是可接受的
        return groups.remove(directory.directory());
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
            // 一台机器的所有服务全部下线才会走到这里, 并发问题(get与remove并不是原子操作)是可接受的
            groupRefCounter.remove(group);
        }
        return count;
    }

    public static class CopyOnWriteGroupList extends CopyOnWriteArrayList<JChannelGroup> {

        private static final long serialVersionUID = -666607632499368496L;

        @Override
        public boolean addIfAbsent(JChannelGroup group) {
            boolean added = super.addIfAbsent(group);
            if (added) {
                incrementRefCount(group);
            }
            return added;
        }

        @Override
        public boolean remove(Object o) {
            boolean removed = super.remove(o);
            if (removed && o instanceof JChannelGroup) {
                decrementRefCount((JChannelGroup) o);
            }
            return removed;
        }

        @Override
        public boolean add(JChannelGroup group) {
            throw new UnsupportedOperationException();
        }
    }

    public static class GroupRefCounterMap extends ConcurrentHashMap<JChannelGroup, AtomicInteger> {

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
}
