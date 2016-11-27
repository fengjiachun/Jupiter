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

import java.util.concurrent.ThreadLocalRandom;

/**
 * Random load balancer with weight.
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public abstract class RandomLoadBalancer<T> implements LoadBalancer<T> {

    private final ThreadLocal<WeightArray> weightsThreadLocal = new ThreadLocal<WeightArray>() {

        @Override
        protected WeightArray initialValue() {
            return new WeightArray();
        }
    };

    @SuppressWarnings("unchecked")
    @Override
    public T select(Object[] elements, MessageWrapper unused) {
        int length = elements.length;
        if (length == 0) {
            throw new IllegalArgumentException("empty elements for select");
        }
        if (length == 1) {
            return (T) elements[0];
        }

        int sumWeight = 0;
        WeightArray weightSnapshots = weightsThreadLocal.get();
        weightSnapshots.refresh(length);
        for (int i = 0; i < length; i++) {
            int val = getWeight((T) elements[i]);
            weightSnapshots.set(i, val);
            sumWeight += val;
        }

        boolean sameWeight = true;
        for (int i = 1; i < length; i++) {
            if (weightSnapshots.get(0) != weightSnapshots.get(i)) {
                sameWeight = false;
                break;
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        // 如果权重不相同且总权重大于0, 则按总权重数随机
        if (!sameWeight && sumWeight > 0) {
            int offset = random.nextInt(sumWeight);
            // 确定随机值落在哪个片
            for (int i = 0; i < length; i++) {
                offset -= weightSnapshots.get(i);
                if (offset < 0) {
                    return (T) elements[i];
                }
            }
        }

        return (T) elements[random.nextInt(length)];
    }

    protected abstract int getWeight(T t);
}
