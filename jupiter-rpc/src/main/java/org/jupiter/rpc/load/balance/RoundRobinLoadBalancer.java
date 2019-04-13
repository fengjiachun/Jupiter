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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.jupiter.transport.Directory;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannelGroup;

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
        if (weightArray == null || weightArray.length() != length) {
            weightArray = WeightSupport.computeWeights(groups, elements, directory);
        }

        int rrIndex = indexUpdater.getAndIncrement(this) & Integer.MAX_VALUE;

        if (weightArray.isAllSameWeight()) {
            return elements[rrIndex % length];
        }

        int nextIndex = getNextServerIndex(weightArray, length, rrIndex);

        return elements[nextIndex];
    }

    private static int getNextServerIndex(WeightArray weightArray, int length, final int rrIndex) {
        int[] weights = new int[length];
        int maxWeight = weights[0] = weightArray.get(0);
        for (int i = 1; i < length; i++) {
            weights[i] = weightArray.get(i) - weightArray.get(i - 1);
            if (weights[i] > maxWeight) {
                maxWeight = weights[i];
            }
        }

        // the greatest common divisor
        int gcd = weightArray.gcd();
        if (gcd < 1) {
            gcd = WeightSupport.n_gcd(weights, length);
            weightArray.gcd(gcd);
        }

        // get next server index
        int sumWeight = weightArray.get(length - 1);
        int val = rrIndex % (sumWeight / gcd);
        for (int i = 0; i < maxWeight; i++) {
            for (int j = 0; j < length; j++) {
                if (val == 0 && weights[j] > 0) {
                    return j;
                }
                if (weights[j] > 0) {
                    weights[j] -= gcd;
                    --val;
                }
            }
        }

        return rrIndex % length;
    }
}
