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

package org.jupiter.example.non.annotation;

import org.jupiter.example.ServiceNonAnnotationTest;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
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
public class JupiterClient {

    public static void main(String[] args) {
        final JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(
                new ServiceMetadata("test", "org.jupiter.example.ServiceNonAnnotationTest", "1.0.0")
        );
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                client.shutdownGracefully();
            }
        });

        ServiceNonAnnotationTest service = ProxyFactory.factory(ServiceNonAnnotationTest.class)
                .group("test")
                .providerName("org.jupiter.example.ServiceNonAnnotationTest")
                .version("1.0.0")
                .client(client)
                .serializerType(SerializerType.PROTO_STUFF)
                .newProxyInstance();

        try {
            String result = service.sayHello("jupiter");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
