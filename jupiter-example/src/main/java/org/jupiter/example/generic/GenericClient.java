/*
 * Copyright (c) 2016 The Jupiter Project
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

package org.jupiter.example.generic;

import org.jupiter.rpc.Directory;
import org.jupiter.rpc.consumer.GenericProxyFactory;
import org.jupiter.rpc.consumer.invoker.GenericInvoker;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

/**
 * jupiter
 * org.jupiter.example.generic
 *
 * @author jiachun.fjc
 */
public class GenericClient {

    public static void main(String[] args) {
        Directory directory = new ServiceMetadata("test", "1.0.0.daily", "GenericServiceTest");

        NettyConnector connector = new JNettyTcpConnector();
        // 连接RegistryServer
        connector.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionManager manager = connector.manageConnections(directory);
        // 等待连接可用
        if (!manager.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        GenericInvoker invoker = GenericProxyFactory.factory()
                .connector(connector)
                .directory(directory)
                .newProxyInstance();

        try {
            Object result = invoker.$invoke("sayHello", "Luca");
            System.out.println(result);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
