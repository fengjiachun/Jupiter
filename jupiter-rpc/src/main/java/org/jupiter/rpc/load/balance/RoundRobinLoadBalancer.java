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

import org.jupiter.common.concurrent.atomic.AtomicUpdater;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannelGroup;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 加权轮训负载均衡
 *
 * 每个服务应有各自独立的实例(index不共享)
 *
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
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {

    private static final AtomicIntegerFieldUpdater<RoundRobinLoadBalancer> indexUpdater =
            AtomicUpdater.newAtomicIntegerFieldUpdater(RoundRobinLoadBalancer.class, "index");

    @SuppressWarnings("unused")
    private volatile int index = 0;

    @Override
    public JChannelGroup select(CopyOnWriteGroupList groups, @SuppressWarnings("unused") MessageWrapper unused) {
        Object[] elements = groups.snapshot();
        int length = elements.length;

        if (length == 0) {
            return null;
        }

        if (length == 1) {
            return (JChannelGroup) elements[0];
        }

        int index = indexUpdater.getAndIncrement(this) & Integer.MAX_VALUE;

        if (groups.isSameWeight()) {
            return (JChannelGroup) elements[index % length];
        }

        // 遍历权重
        boolean allWarmUpComplete = true;
        int sumWeight = 0;
        WeightArray weightsSnapshot = weightArray(length);
        for (int i = 0; i < length; i++) {
            JChannelGroup group = (JChannelGroup) elements[i];

            int val = getWeight(group);

            weightsSnapshot.set(i, val);
            sumWeight += val;
            allWarmUpComplete = (allWarmUpComplete && group.isWarmUpComplete());
        }

        int maxWeight = 0;
        int minWeight = Integer.MAX_VALUE;
        for (int i = 0; i < length; i++) {
            int val = weightsSnapshot.get(i);
            maxWeight = Math.max(maxWeight, val);
            minWeight = Math.min(minWeight, val);
        }

        if (allWarmUpComplete && maxWeight > 0 && minWeight == maxWeight) {
            // 预热全部完成并且权重完全相同
            groups.setSameWeight(true);
        }

        if (maxWeight > 0 && minWeight < maxWeight) {
            int mod = index % sumWeight;
            for (int i = 0; i < maxWeight; i++) {
                for (int j = 0; j < length; j++) {
                    int val = weightsSnapshot.get(j);
                    if (mod == 0 && val > 0) {
                        return (JChannelGroup) elements[j];
                    }
                    if (val > 0) {
                        weightsSnapshot.set(j, val - 1);
                        --mod;
                    }
                }
            }
        }

        return (JChannelGroup) elements[index % length];
    }
}
