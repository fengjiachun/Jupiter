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
public class RoundRobinLoadBalancerTest {

    public static void main(String[] args) {
        int len = 50;
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            Channel c = new Channel();
            c.index = i;
            c.weight = 1;
            array[i] = c;
        }
        ((Channel) array[15]).weight = 3;

        LoadBalancer<Channel> lb = new RR();
        len += 2;
        for (int i = 0; i < len; i++) {
            System.out.println(lb.select(array, null));
        }
    }
}

class RR extends RoundRobinLoadBalancer<Channel> {

    @Override
    protected int getWeight(Channel channel) {
        return channel.weight;
    }
}

