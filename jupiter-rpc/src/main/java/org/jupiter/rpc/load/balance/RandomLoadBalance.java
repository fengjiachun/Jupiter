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

import org.jupiter.common.util.Reflects;

import java.lang.reflect.Field;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.jupiter.common.util.internal.UnsafeAccess.UNSAFE;

/**
 * Random load balance with weight.
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public abstract class RandomLoadBalance<T> implements LoadBalance<T> {

    private static final long ELEMENTS_OFFSET;
    static {
        long offset;
        try {
            Field field = Reflects.getField(CopyOnWriteArrayList.class, "array");
            offset = UNSAFE.objectFieldOffset(field);
        } catch (Exception e) {
            offset = 0;
        }
        ELEMENTS_OFFSET = offset;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T select(CopyOnWriteArrayList<T> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("[LoadBalance] empty list for select");
        }

        // 请原谅下面这段放荡不羁的糟糕代码
        Object[] array; // The snapshot of elements array
        if (ELEMENTS_OFFSET > 0) {
            array = (Object[]) UNSAFE.getObjectVolatile(list, ELEMENTS_OFFSET);
        } else {
            array = (Object[]) Reflects.getValue(list, "array");
        }
        int length = array.length;
        if (length == 1) {
            return (T) array[0];
        }

        int totalWeight = 0;
        int[] weightSnapshots = new int[length];
        boolean isSameWeight = true;
        for (int i = 0; i < length; i++) {
            int weight = getWeight((T) array[i]);
            weightSnapshots[i] = weight;
            totalWeight += weight;

            if (isSameWeight && i > 0 && weight != getWeight((T) array[i - 1])) {
                isSameWeight = false;
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (totalWeight > 0 && !isSameWeight) {
            // 如果权重不相同且权重大于0, 则按总权重数随机
            int offset = random.nextInt(totalWeight);

            // 确定随机值落在哪个片
            for (int i = 0; i < length; i++) {
                offset -= weightSnapshots[i];
                if (offset < 0) {
                    return (T) array[i];
                }
            }
        }

        return (T) array[random.nextInt(length)];
    }

    protected abstract int getWeight(T t);
}
