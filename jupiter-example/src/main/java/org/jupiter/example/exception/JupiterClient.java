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
package org.jupiter.example.exception;

import org.jupiter.example.ExceptionServiceTest;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
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
        JConnector.ConnectionWatcher watcher = client.watchConnections(ExceptionServiceTest.class, "1.0.0.daily");
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(client::shutdownGracefully));

        ExceptionServiceTest service = ProxyFactory.factory(ExceptionServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .serializerType(SerializerType.PROTO_STUFF)
                .failoverRetries(5)
                .newProxyInstance();

        try {
            service.hello(1);
        } catch (Exception e) {
            System.err.println("remote stack trace =========================");
            e.printStackTrace();
            System.err.println("=========================");
            System.err.println("expected message: " + e.getMessage());
        }

        try {
            service.hello(0);
        } catch (Exception e) {
            System.err.println("local stack trace =========================");
            e.printStackTrace();
            System.err.println("=========================");
            System.err.println("unexpected message: " + e.getMessage());
        }
    }
}
