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

import java.util.concurrent.ThreadLocalRandom;

import org.jupiter.transport.Directory;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannelGroup;

/**
 * 加权随机负载均衡.
 *
 * <pre>
 * *****************************************************************************
 *
 *            random value
 * ─────────────────────────────────▶
 *                                  │
 *                                  ▼
 * ┌─────────────────┬─────────┬──────────────────────┬─────┬─────────────────┐
 * │element_0        │element_1│element_2             │...  │element_n        │
 * └─────────────────┴─────────┴──────────────────────┴─────┴─────────────────┘
 *
 * *****************************************************************************
 * </pre>
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public class RandomLoadBalancer implements LoadBalancer {

    private static final RandomLoadBalancer instance = new RandomLoadBalancer();

    public static RandomLoadBalancer instance() {
        return instance;
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

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (weightArray.isAllSameWeight()) {
            return elements[random.nextInt(length)];
        }

        int nextIndex = getNextServerIndex(weightArray, length, random);

        return elements[nextIndex];
    }

    private static int getNextServerIndex(WeightArray weightArray, int length, ThreadLocalRandom random) {
        int sumWeight = weightArray.get(length - 1);
        int val = random.nextInt(sumWeight + 1);
        return WeightSupport.binarySearchIndex(weightArray, length, val);
    }
}
