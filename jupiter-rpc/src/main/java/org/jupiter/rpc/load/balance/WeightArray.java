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

/**
 * 通常负载均衡算法每次都要重新获取所有可用服务的权重信息(由于预热的关系权重可能一直在变化着),
 * {@link WeightArray} 存在的意义是尽量减少内存的占用(结构简单),
 * 再配合ThreadLocal使用, 有助于减少大量临时的短生命周期对象对GC的影响.
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
final class WeightArray {

    private int[] array = new int[64];

    int get(int index) {
        return array[index];
    }

    void set(int index, int value) {
        array[index] = value;
    }

    WeightArray refresh(int capacity) {
        if (capacity > array.length) {
            array = new int[capacity];
        }
        return this;
    }
}
