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

/**
 * Load balancer.
 *
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public interface LoadBalancer<T> {

    /**
     * Select one in elements array.
     *
     * @param elements  所有可选择的元素, Object[]设计很糟糕, 纯是为了性能做出的让步
     * @param message   通常这个参数无用, 对于一致性hash等负载均衡算法会有用, 先占位留作扩展
     */
    T select(Object[] elements, MessageWrapper message);
}
