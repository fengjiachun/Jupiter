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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 按照权重轮训负载均衡, 每个服务应该有一个独立的load balancer实例(index不应该共享)
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public abstract class RoundRobinLoadBalancer<T> implements LoadBalancer<T> {

    private static final AtomicIntegerFieldUpdater<RoundRobinLoadBalancer> indexUpdater =
            AtomicUpdater.newAtomicIntegerFieldUpdater(RoundRobinLoadBalancer.class, "index");

    @SuppressWarnings("unused")
    private volatile int index = 0;

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

        int maxWeight = 0;
        int minWeight = Integer.MAX_VALUE;
        for (int i = 0; i < length; i++) {
            int val = weightSnapshots.get(i);
            maxWeight = Math.max(maxWeight, val);
            minWeight = Math.min(minWeight, val);
        }

        int index = indexUpdater.getAndIncrement(this) & Integer.MAX_VALUE;
        if (maxWeight > 0 && minWeight < maxWeight) {
            int mod = index % sumWeight;
            for (int i = 0; i < maxWeight; i++) {
                for (int j = 0; j < length; j++) {
                    int val = weightSnapshots.get(j);
                    if (mod == 0 && val > 0) {
                        return (T) elements[j];
                    }
                    if (val > 0) {
                        weightSnapshots.set(j, val - 1);
                        --mod;
                    }
                }
            }
        }

        return (T) elements[index % length];
    }

    protected abstract int getWeight(T t);
}
