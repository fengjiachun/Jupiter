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
package org.jupiter.example.generic;

import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.GenericProxyFactory;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.rpc.consumer.invoker.GenericInvoker;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.Directory;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * jupiter
 * org.jupiter.example.generic
 *
 * @author jiachun.fjc
 */
public class GenericJupiterClient {

    public static void main(String[] args) {
        Directory directory = new ServiceMetadata("test", "GenericServiceTest", "1.0.0.daily");

        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(directory);
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        GenericInvoker invoker = GenericProxyFactory.factory()
                .client(client)
                .directory(directory)
                .invokeType(InvokeType.ASYNC)
                .newProxyInstance();

        try {
            Object result = invoker.$invoke("sayHello", "Luca");
            System.out.println(result);
            InvokeFuture<Object> future = InvokeFutureContext.future(Object.class);
            System.out.println(future.getResult());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
