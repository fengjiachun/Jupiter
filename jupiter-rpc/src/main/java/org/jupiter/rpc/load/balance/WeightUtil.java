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

import org.jupiter.common.util.SystemClock;
import org.jupiter.transport.Directory;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannelGroup;

/**
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
final class WeightUtil {

    private static final ThreadLocal<WeightArray> weightsThreadLocal = new ThreadLocal<WeightArray>() {

        @Override
        protected WeightArray initialValue() {
            return new WeightArray();
        }
    };

    static WeightArray computeWeightArray(
            CopyOnWriteGroupList groups, JChannelGroup[] elements, Directory directory, int length) {

        WeightArray weightArray = weightsThreadLocal.get().refresh(length);

        boolean allWarmUpComplete = true;
        boolean allSameWeight = true;
        for (int i = 0; i < length; i++) {
            allWarmUpComplete = (allWarmUpComplete && elements[i].isWarmUpComplete());

            int preVal = 0;
            int curVal = getWeight(elements[i], directory);
            if (i > 0) {
                preVal = weightArray.get(i - 1);
                allSameWeight = allSameWeight && preVal == curVal;
            }

            weightArray.set(i, preVal + curVal);
        }

        if (allWarmUpComplete) {
            if (allSameWeight) {
                weightArray.setAllSameWeight(true);
                groups.setWeightInfo(elements, directory, weightArray);
            }
        }

        return weightArray;
    }

    // 计算权重, 包含预热逻辑
    private static int getWeight(JChannelGroup group, Directory directory) {
        int weight = group.getWeight(directory);
        int warmUp = group.getWarmUp();
        int upTime = (int) (SystemClock.millisClock().now() - group.timestamp());

        if (upTime > 0 && upTime < warmUp) {
            // 对端服务预热中, 计算预热权重
            weight = (int) (((float) upTime / warmUp) * weight);
        }

        return weight > 0 ? weight : 0;
    }

    static int binarySearchIndex(WeightArray weightArray, int length, int value) {
        int low = 0;
        int high = length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = weightArray.get(mid);

            if (midVal < value) {
                low = mid + 1;
            } else if (midVal > value) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        return low;
    }
}
