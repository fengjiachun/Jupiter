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
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public final class LoadBalancerFactory {

    public static LoadBalancer loadBalancer(LoadBalancerType type) {
        if (type == LoadBalancerType.RANDOM) {
            return RandomLoadBalancer.instance();
        }

        if (type == LoadBalancerType.ROUND_ROBIN) {
            return RoundRobinLoadBalancer.instance();
        }

        // 如果不指定, 默认的负载均衡算法是加权随机
        return RandomLoadBalancer.instance();
    }

    private LoadBalancerFactory() {}
}
