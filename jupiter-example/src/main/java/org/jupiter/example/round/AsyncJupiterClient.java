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

import org.jupiter.example.User;
import org.jupiter.example.UserService;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class AsyncJupiterClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(UserService.class, "1.0.0.daily");
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        UserService userService = ProxyFactory.factory(UserService.class)
                .version("1.0.0.daily")
                .client(client)
                .invokeType(InvokeType.ASYNC)
                .newProxyInstance();

        try {
            User user = userService.createUser();
            System.out.println("sync result: " + user);
            InvokeFuture<User> future = InvokeFutureContext.future(User.class);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    System.out.println("callback: " + result);
                } else {
                    throwable.printStackTrace();
                }
            });
            System.out.println("future.get: " + future.getResult());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
