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
package org.jupiter.tracing;

import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.serialization.SerializerType;
import org.jupiter.tracing.service.TracingService1;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * Client1 --> Server1AndClient2 --> Server2
 *
 * 1. 先启动 JupiterRegistryServer
 * 2. 再启动 Server2
 * 3. 接着启动 Server1AndClient2
 * 4. 最后启动 Client1
 *
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
public class Client1 {

    public static void main(String[] args) {
        OpenTracingContext.setTracerFactory(new TestTracerFactory());

        final JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());

        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(TracingService1.class, "1.0.0.daily");
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        TracingService1 service = ProxyFactory.factory(TracingService1.class)
                .version("1.0.0.daily")
                .client(client)
                .serializerType(SerializerType.JAVA)
                .clusterStrategy(ClusterInvoker.Strategy.FAIL_OVER)
                .failoverRetries(5)
                .newProxyInstance();

        try {
            System.out.println(service.call1("test1"));
            System.out.println(service.call1("test2"));
            System.out.println(service.call1("test3"));
            System.out.println(service.call1("test4"));
            System.out.println(service.call1("test5"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
