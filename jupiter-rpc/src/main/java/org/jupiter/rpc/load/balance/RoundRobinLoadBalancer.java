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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 轮训负载均衡, 每个服务应该有一个独立的load balancer实例(index不应该共享)
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public class RoundRobinLoadBalancer<T> implements LoadBalancer<T> {

    private static final AtomicIntegerFieldUpdater<RoundRobinLoadBalancer> indexUpdater =
            AtomicUpdater.newAtomicIntegerFieldUpdater(RoundRobinLoadBalancer.class, "index");

    @SuppressWarnings("unused")
    private volatile int index = 0;

    @SuppressWarnings("unchecked")
    @Override
    public T select(Object[] elements) {
        int length = elements.length;
        if (length == 0) {
            throw new IllegalArgumentException("empty elements for select");
        }
        if (length == 1) {
            return (T) elements[0];
        }

        int offset = Math.abs(indexUpdater.getAndIncrement(this) % length);
        return (T) elements[offset];
    }
}
