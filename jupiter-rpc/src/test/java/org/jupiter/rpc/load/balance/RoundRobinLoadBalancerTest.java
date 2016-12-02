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

package org.jupiter.rpc.load.balance;

import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.DirectoryJChannelGroup;

/**
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public class RoundRobinLoadBalancerTest {

    public static void main(String[] args) {
        CopyOnWriteGroupList groupList = new CopyOnWriteGroupList(new DirectoryJChannelGroup());
        int len = 50;
        for (int i = 0; i < len; i++) {
            ChannelGroup c = new ChannelGroup();
            c.index = i;
            c.weight = 1;
            groupList.addIfAbsent(c);
        }
        groupList.get(15).setWeight(3);

        LoadBalancer lb = new RoundRobinLoadBalancer();
        len += 2;
        for (int i = 0; i < len; i++) {
            System.out.print((i + 1) + " ");
            System.out.println(lb.select(groupList, null));
        }
    }
}

