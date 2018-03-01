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

package org.jupiter.example.round;

import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.example.ServiceTest;
import org.jupiter.example.ServiceTest2;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class SyncJupiterClient {

    public static void main(String[] args) {
        SystemPropertyUtil.setProperty("jupiter.transport.decode.low_copy", "true");
        SystemPropertyUtil.setProperty("jupiter.transport.encode.low_copy", "true");

        final JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        final MonitorServer monitor = new MonitorServer(19991);
        monitor.setJupiterClient(client);
        try {
            monitor.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher1 = client.watchConnections(ServiceTest.class, "1.0.0.daily");
        JConnector.ConnectionWatcher watcher2 = client.watchConnections(ServiceTest2.class, "1.0.0.daily");
        // 等待连接可用
        if (!watcher1.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }
        if (!watcher2.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                client.shutdownGracefully();
            }
        });

        ServiceTest service1 = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .serializerType(SerializerType.KRYO)
                .clusterStrategy(ClusterInvoker.Strategy.FAIL_OVER)
                .failoverRetries(5)
                .newProxyInstance();

        ServiceTest2 service2 = ProxyFactory.factory(ServiceTest2.class)
                .version("1.0.0.daily")
                .client(client)
                .serializerType(SerializerType.KRYO)
                .newProxyInstance();

        try {
            ServiceTest.ResultClass result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);
            result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);
            result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);
            result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);
            result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);
            result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);
            result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);
            result1 = service1.sayHello("jupiter", "hello");
            System.out.println(result1);

            String result2 = service2.sayHelloString();
            System.out.println(result2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
