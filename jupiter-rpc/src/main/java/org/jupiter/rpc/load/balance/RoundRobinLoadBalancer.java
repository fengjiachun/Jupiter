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

import org.jupiter.transport.Directory;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannelGroup;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 加权轮询负载均衡.
 *
 * 当前实现不会先去计算最大公约数再轮询, 通常最大权重和最小权重值不会相差过于悬殊,
 * 因此我觉得没有必要先去求最大公约数, 很可能产生没有必要的开销.
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
            AtomicIntegerFieldUpdater.newUpdater(RoundRobinLoadBalancer.class, "index");

    @SuppressWarnings("unused")
    private volatile int index = 0;

    public static RoundRobinLoadBalancer instance() {
        // round-robin是有状态(index)的, 不能是单例
        return new RoundRobinLoadBalancer();
    }

    @Override
    public JChannelGroup select(CopyOnWriteGroupList groups, Directory directory) {
        JChannelGroup[] elements = groups.snapshot();
        int length = elements.length;

        if (length == 0) {
            return null;
        }

        if (length == 1) {
            return elements[0];
        }

        int index = indexUpdater.getAndIncrement(this) & Integer.MAX_VALUE;

        if (groups.isSameWeight()) {
            // 对于大多数场景, 在预热都完成后, 很可能权重都是相同的, 那么加权轮询算法将是没有必要的开销,
            // 如果发现一个CopyOnWriteGroupList里面所有元素权重相同, 会设置一个sameWeight标记,
            // 下一次直接退化到普通随机算法, 如果CopyOnWriteGroupList中元素出现变化, 标记会被自动取消.
            return elements[index % length];
        }

        // 遍历权重
        boolean allWarmUpComplete = true;
        int sumWeight = 0;
        WeightArray weightsSnapshot = weightArray(length);
        for (int i = 0; i < length; i++) {
            JChannelGroup group = elements[i];

            int val = getWeight(group, directory);

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

        // 这一段算法参考当前的类注释中的那张图
        //
        // 当前实现不会先去计算最大公约数再轮询, 通常最大权重和最小权重值不会相差过于悬殊,
        // 因此我觉得没有必要先去求最大公约数, 很可能产生没有必要的开销.
        if (maxWeight > 0 && minWeight < maxWeight) {
            int mod = index % sumWeight;
            for (int i = 0; i < maxWeight; i++) {
                for (int j = 0; j < length; j++) {
                    int val = weightsSnapshot.get(j);
                    if (mod == 0 && val > 0) {
                        return elements[j];
                    }
                    if (val > 0) {
                        weightsSnapshot.set(j, val - 1);
                        --mod;
                    }
                }
            }
        }

        return elements[index % length];
    }
}
