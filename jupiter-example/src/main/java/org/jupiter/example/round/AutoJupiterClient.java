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

import java.util.concurrent.CompletableFuture;

import org.jupiter.example.AsyncUserService;
import org.jupiter.example.User;
import org.jupiter.example.UserService;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class AutoJupiterClient {

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

        AsyncUserService userService = ProxyFactory.factory(AsyncUserService.class)
                .version("1.0.0.daily")
                .client(client)
                .invokeType(InvokeType.AUTO)
                .newProxyInstance();

        try {
            CompletableFuture<User> f = userService.createUser();
            System.out.println("CompletableFuture.isDone: " + f.isDone());
            f.whenComplete((user, throwable) -> System.out.println("when complete: " + user));
            System.out.println("CompletableFuture.get: " + f.get());
            System.out.println("CompletableFuture.isDone: " + f.isDone());

            AsyncUserService.MyCompletableFuture<User> mf = userService.createUser2();

            System.out.println("MyCompletableFuture.isDone: " + mf.isDone());
            mf.whenComplete((user2, throwable) -> System.out.println("when complete: " + user2));
            System.out.println("MyCompletableFuture.get: " + mf.get());
            System.out.println("MyCompletableFuture.isDone: " + mf.isDone());

            System.out.println("sync invoke: " + userService.syncCreateUser());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
