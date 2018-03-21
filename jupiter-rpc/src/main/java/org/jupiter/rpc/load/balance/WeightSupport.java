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

import static java.lang.Math.min;

/**
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
final class WeightSupport {

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

    static WeightArray computeWeights(CopyOnWriteGroupList groups, JChannelGroup[] elements, Directory directory) {
        int length = elements.length;
        int[] weights = new int[length];

        boolean allWarmUpComplete = elements[0].isWarmUpComplete();
        boolean allSameWeight = true;
        weights[0] = getWeight(elements[0], directory);
        for (int i = 1; i < length; i++) {
            allWarmUpComplete &= elements[i].isWarmUpComplete();
            weights[i] = getWeight(elements[i], directory);
            allSameWeight &= (weights[i - 1] == weights[i]);
        }

        if (allWarmUpComplete && allSameWeight) {
            weights = null;
        }

        if (weights != null) {
            weights = weightsDivisionGcd(weights, length);
            for (int i = 1; i < length; i++) {
                // [curVal += preVal] for binary search
                weights[i] += weights[i - 1];
            }
        }

        WeightArray weightArray = new WeightArray(weights);

        if (allWarmUpComplete) {
            groups.setWeightArray(elements, directory.directoryString(), weightArray);
        }

        return weightArray;
    }

    // 计算权重, 包含预热逻辑
    static int getWeight(JChannelGroup group, Directory directory) {
        int weight = group.getWeight(directory);
        int warmUp = group.getWarmUp();
        int upTime = (int) (SystemClock.millisClock().now() - group.timestamp());

        if (upTime > 0 && upTime < warmUp) {
            // 对端服务预热中, 计算预热权重
            weight = (int) (((float) upTime / warmUp) * weight);
        }

        return weight > 0 ? weight : 0;
    }

    static int[] weightsDivisionGcd(int[] weights, int n) {
        int gcd = n_gcd(weights, n);
        if (gcd > 1) {
            for (int i = 0; i < n; i++) {
                weights[i] = weights[i] / gcd;
            }
        }
        return weights;
    }

    static int n_gcd(int[] array, int n) {
        if (n == 1) {
            return array[0];
        }
        return gcd(array[n - 1], n_gcd(array, n - 1));
    }

    static int gcd(int a, int b) {
        if (a == 0) {
            return b;
        } else if (b == 0) {
            return a; // similar logic
        }

        int aTwos = Integer.numberOfTrailingZeros(a);
        a >>= aTwos; // divide out all 2s
        int bTwos = Integer.numberOfTrailingZeros(b);
        b >>= bTwos; // divide out all 2s
        while (a != b) { // both a, b are odd
            int delta = a - b; // can't overflow, since a and b are nonnegative
            int minDeltaOrZero = delta & (delta >> (Integer.SIZE - 1));
            a = delta - minDeltaOrZero - minDeltaOrZero; // sets a to Math.abs(a - b)
            b += minDeltaOrZero; // sets b to min(old a, b)
            a >>= Integer.numberOfTrailingZeros(a); // divide out all 2s, since 2 doesn't divide b
        }
        return a << min(aTwos, bTwos);
    }

    private WeightSupport() {}
}
