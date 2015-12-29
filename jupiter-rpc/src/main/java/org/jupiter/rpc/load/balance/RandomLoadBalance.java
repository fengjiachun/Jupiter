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

    @SuppressWarnings({"unchecked", "ForLoopReplaceableByForEach"})
    @Override
    public T select(CopyOnWriteArrayList<T> list) {
        // 请原谅下面这段放荡不羁的糟糕代码
        Object[] array; // The snapshot of elements array
        if (ELEMENTS_OFFSET > 0) {
            array = (Object[]) UNSAFE.getObjectVolatile(list, ELEMENTS_OFFSET);
        } else {
            array = (Object[]) Reflects.getValue(list, "array");
        }

        final int arrayLength = array.length;
        if (arrayLength == 0) {
            throw new IllegalArgumentException("empty elements for select");
        }
        if (arrayLength == 1) {
            return (T) array[0];
        }

        int totalWeight = 0;
        int[] weightSnapshots = new int[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            totalWeight += (weightSnapshots[i] = getWeight((T) array[i]));
        }

        boolean sameWeight = true;
        for (int i = 1; i < arrayLength; i++) {
            if (weightSnapshots[0] != weightSnapshots[i]) {
                sameWeight = false;
                break;
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        // 如果权重不相同且总权重大于0, 则按总权重数随机
        if (!sameWeight && totalWeight > 0) {
            int offset = random.nextInt(totalWeight);
            // 确定随机值落在哪个片
            for (int i = 0; i < arrayLength; i++) {
                offset -= weightSnapshots[i];
                if (offset < 0) {
                    return (T) array[i];
                }
            }
        }

        return (T) array[random.nextInt(arrayLength)];
    }

    protected abstract int getWeight(T t);
}
