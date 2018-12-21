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

package org.jupiter.example.spring.interceptor.consumer;

import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.consumer.ConsumerInterceptor;
import org.jupiter.transport.channel.JChannel;

/**
 * jupiter
 * org.jupiter.example.spring
 *
 * @author jiachun.fjc
 */
public class MyConsumerInterceptor2 implements ConsumerInterceptor {

    @Override
    public void beforeInvoke(JRequest request, JChannel channel) {
        System.err.println("2 beforeInvoke#" + request + " channel=" + channel);
    }

    @Override
    public void afterInvoke(JResponse response, JChannel channel) {
        System.err.println("2 afterInvoke#" + response + " channel=" + channel);
    }
}
