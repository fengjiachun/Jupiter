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
 * 每个服务应有各自独立的实例(index不共享)
 *
 * <pre>
 * **********************************************************************
 *
 *  index++ % (sumWeight / gcd)
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

    private static final AtomicIntegerFieldUpdater<RoundRobinLoadBalancer> indexUpdater =
            AtomicIntegerFieldUpdater.newUpdater(RoundRobinLoadBalancer.class, "index");

    @SuppressWarnings("unused")
    private volatile int index = 0;

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
            weightArray = WeightSupport.computeWeights(groups, elements, directory);
        }

        int index = indexUpdater.getAndIncrement(this) & Integer.MAX_VALUE;

        if (weightArray.isAllSameWeight()) {
            return elements[index % length];
        }

        // defensive fault tolerance
        length = Math.min(length, weightArray.length());

        int[] weightsSnapshot = new int[length];
        int maxWeight = weightsSnapshot[0] = weightArray.get(0);
        for (int i = 1; i < length; i++) {
            weightsSnapshot[i] = weightArray.get(i) - weightArray.get(i - 1);
            if (weightsSnapshot[i] > maxWeight) {
                maxWeight = weightsSnapshot[i];
            }
        }

        // the greatest common divisor
        int gcd = weightArray.gcd();
        if (gcd < 1) {
            gcd = WeightSupport.n_gcd(weightArray.array(), length);
            if (length == weightArray.length()) {
                weightArray.gcd(gcd);
            }
        }

        // 这一段算法参考当前的类注释中的那张图
        int sumWeight = weightArray.get(length - 1);
        int eVal = index % (sumWeight / gcd);
        for (int i = 0; i < maxWeight; i++) {
            for (int j = 0; j < length; j++) {
                if (eVal == 0 && weightsSnapshot[j] > 0) {
                    return elements[j];
                }
                if (weightsSnapshot[j] > 0) {
                    weightsSnapshot[j] -= gcd;
                    --eVal;
                }
            }
        }

        return elements[index % length];
    }
}
