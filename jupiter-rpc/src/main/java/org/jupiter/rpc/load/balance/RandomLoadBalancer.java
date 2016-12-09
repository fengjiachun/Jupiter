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

import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannelGroup;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 加权随机负载均衡
 *
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
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public class RandomLoadBalancer extends AbstractLoadBalancer {

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

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (groups.isSameWeight()) {
            return (JChannelGroup) elements[random.nextInt(length)];
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

        boolean sameWeight = true;
        int val_0 = weightsSnapshot.get(0);
        for (int i = 1; i < length && sameWeight; i++) {
            sameWeight = (val_0 == weightsSnapshot.get(i));
        }

        if (allWarmUpComplete && sameWeight) {
            // 预热全部完成并且权重完全相同
            groups.setSameWeight(true);
        }

        // 如果权重不相同且总权重大于0, 则按总权重数随机
        if (!sameWeight && sumWeight > 0) {
            int offset = random.nextInt(sumWeight);
            // 确定随机值落在哪个片
            for (int i = 0; i < length; i++) {
                offset -= weightsSnapshot.get(i);
                if (offset < 0) {
                    return (JChannelGroup) elements[i];
                }
            }
        }

        return (JChannelGroup) elements[random.nextInt(length)];
    }
}
