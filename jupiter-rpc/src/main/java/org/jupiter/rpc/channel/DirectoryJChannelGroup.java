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

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * jupiter
 * org.jupiter.rpc.channel
 *
 * @author jiachun.fjc
 */
public class DirectoryJChannelGroup {

    private final ConcurrentMap<String, CopyOnWriteArrayList<JChannelGroup>> groups = Maps.newConcurrentHashMap();

    public CopyOnWriteArrayList<JChannelGroup> list(Directory directory) {
        final String _directory = directory.directory();

        CopyOnWriteArrayList<JChannelGroup> groupList = groups.get(_directory);
        if (groupList == null) {
            CopyOnWriteArrayList<JChannelGroup> newGroupList = new CopyOnWriteArrayList<>();
            groupList = groups.putIfAbsent(_directory, newGroupList);
            if (groupList == null) {
                groupList = newGroupList;
            }
        }

        return groupList;
    }
}
