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
        String _directory = directory.directory();

        CopyOnWriteArrayList<JChannelGroup> groupList = groups.get(_directory);
        if (groupList == null) {
            CopyOnWriteArrayList<JChannelGroup> newGroupList = new CopyOnWriteArrayList<JChannelGroup>();
            groupList = groups.putIfAbsent(_directory, newGroupList);
            if (groupList == null) {
                groupList = newGroupList;
            }
        }

        return groupList;
    }
}
