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
import org.jupiter.transport.channel.JChannelGroup;

/**
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public abstract class AbstractLoadBalancer implements LoadBalancer {

    private static final ThreadLocal<WeightArray> weightsThreadLocal = new ThreadLocal<WeightArray>() {

        @Override
        protected WeightArray initialValue() {
            return new WeightArray();
        }
    };

    protected WeightArray weightArray(int length) {
        return weightsThreadLocal.get().refresh(length);
    }

    protected int getWeight(JChannelGroup group) {
        int weight = group.getWeight();
        if (weight > 0) {
            long timestamp = group.getTimestamp();
            if (timestamp > 0L) {
                int upTime = (int) (SystemClock.millisClock().now() - timestamp);
                int warmUp = group.getWarmUp();

                if (upTime > 0 && upTime < warmUp) {
                    int warmUpWeight = (int) (((float) upTime / warmUp) * weight);
                    return warmUpWeight < 1 ? 1 : (warmUpWeight > weight ? weight : warmUpWeight);
                }

                if (upTime >= warmUp) {
                    group.clearTimestamp();
                }
            }
        }

        return weight > 0 ? weight : 0;
    }
}
