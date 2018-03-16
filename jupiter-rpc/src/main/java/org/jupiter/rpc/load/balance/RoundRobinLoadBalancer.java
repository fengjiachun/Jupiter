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

import org.jupiter.common.util.IntSequence;
import org.jupiter.transport.Directory;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannelGroup;

/**
 * 加权轮询负载均衡.
 *
 * 当前实现不会先去计算最大公约数再轮询, 通常最大权重和最小权重值不会相差过于悬殊,
 * 因此我觉得没有必要先去求最大公约数, 很可能产生没有必要的开销.
 *
 * 每个服务应有各自独立的实例(sequence不共享)
 *
 * <pre>
 * **********************************************************************
 *
 *  index++ % sumWeight
 *
 *                       ┌─┐
 *                       │ │
 *                       │ │                 ┌─┐
 *             ┌─┐       │ │                 │ │
 *             │ │       │ │                 │ │
 *             │ │       │ │                 │ │
 *             │ │       │ │  ┌─┐       ┌─┐  │ │
 * ════════════╬═╬═══════╬═╬══╬═╬═══════╬═╬▶ │ │
 *        ┌─┐  │ │       │ │  │ │  ┌─┐  │ │  │ │
 *        │ │  │ │       │ │  │ │  │ │  │ │  │ │
 * ═══════╬═╬══╬═╬═══════╬═╬══╬═╬══╬═╬══╬═╬══╬═╬══▶
 *        │ │  │ │       │ │  │ │  │ │  │ │  │ │
 *        │ │  │ │  ┌─┐  │ │  │ │  │ │  │ │  │ │
 * ═══════╬═╬══╬═╬══╬═╬══╬═╬══╬═╬══╬═╬══╬═╬══╬═╬══▶
 *        │ │  │ │  │ │  │ │  │ │  │ │  │.│  │ │
 *        │ │  │ │  │ │  │ │  │ │  │ │  │.│  │ │
 *        │0│  │1│  │2│  │3│  │4│  │5│  │.│  │n│
 *        └─┘  └─┘  └─┘  └─┘  └─┘  └─┘  └─┘  └─┘
 *
 * **********************************************************************
 * </pre>
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final IntSequence sequence = new IntSequence();

    public static RoundRobinLoadBalancer instance() {
        // round-robin是有状态的, 不能是单例
        return new RoundRobinLoadBalancer();
    }

    @Override
    public JChannelGroup select(CopyOnWriteGroupList groups, Directory directory) {
        JChannelGroup[] elements = groups.getSnapshot();
        int length = elements.length;

        if (length == 0) {
            return null;
        }

        if (length == 1) {
            return elements[0];
        }

        WeightArray weightArray = (WeightArray) groups.getWeightArray(elements, directory.directoryString());
        if (weightArray == null) {
            weightArray = WeightArray.computeWeightArray(groups, elements, directory, length);
        }

        int index = sequence.next() & Integer.MAX_VALUE;

        if (weightArray.isAllSameWeight()) {
            return elements[index % length];
        }

        int sumWeight = weightArray.get(length - 1);
        int eVal = index % sumWeight;
        int eIndex = WeightArray.binarySearchIndex(weightArray, length, eVal);

        return elements[eIndex];
    }
}
