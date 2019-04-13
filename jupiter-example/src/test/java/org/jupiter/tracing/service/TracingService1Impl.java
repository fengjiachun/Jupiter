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
package org.jupiter.tracing.service;

import org.jupiter.rpc.ServiceProviderImpl;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.serialization.SerializerType;
import org.jupiter.tracing.Server1AndClient2;

/**
 * jupiter
 * org.jupiter.tracing.service
 *
 * @author jiachun.fjc
 */
@ServiceProviderImpl(version = "1.0.0.daily")
public class TracingService1Impl implements TracingService1 {

    private TracingService2 tracingService2 = ProxyFactory.factory(TracingService2.class)
            .version("1.0.0.daily")
            .client(Server1AndClient2.client)
            .serializerType(SerializerType.JAVA)
            .clusterStrategy(ClusterInvoker.Strategy.FAIL_OVER)
            .failoverRetries(5)
            .newProxyInstance();

    @Override
    public String call1(String text) {
        return "Hello call1 [" + tracingService2.call2(text) + "]";
    }
}
