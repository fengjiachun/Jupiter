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

package org.jupiter.rpc;

import org.jupiter.registry.NotifyListener;
import org.jupiter.registry.OfflineListener;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.Registry;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JClient extends Registry {

    JChannelGroup group(UnresolvedAddress address);

    Collection<JChannelGroup> groups();

    void addGroup(Directory directory, JChannelGroup group);

    CopyOnWriteArrayList<JChannelGroup> directory(Directory directory);

    boolean isDirectoryAvailable(Directory directory);

    JChannel select(Directory directory);

    Collection<RegisterMeta> lookup(Directory directory);

    /**
     * 订阅服务
     */
    void subscribe(Directory directory, NotifyListener listener);

    /**
     * Provider下线通知
     */
    void offlineListening(UnresolvedAddress address, OfflineListener listener);
}
