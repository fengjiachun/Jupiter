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
package org.jupiter.example.cluster.failfast;

import org.jupiter.example.cluster.service.ClusterService;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * jupiter
 * org.jupiter.example.cluster.failover
 *
 * @author jiachun.fjc
 */
public class FailfastJupiterClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(ClusterService.class, "1.0.0");
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        // 同步调用
        System.err.println("同步调用fail-fast测试...........");
        ClusterService syncService = ProxyFactory.factory(ClusterService.class)
                .version("1.0.0")
                .client(client)
                .invokeType(InvokeType.SYNC)
                .clusterStrategy(ClusterInvoker.Strategy.FAIL_FAST)
                .newProxyInstance();

        try {
            System.err.println("Sync result=" + syncService.helloString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 异步调用
        System.err.println("异步调用fail-fast测试...........");
        ClusterService asyncService = ProxyFactory.factory(ClusterService.class)
                .version("1.0.0")
                .client(client)
                .invokeType(InvokeType.ASYNC)
                .clusterStrategy(ClusterInvoker.Strategy.FAIL_FAST)
                .newProxyInstance();

        try {
            System.out.println(asyncService.helloString());
            InvokeFuture<String> future = InvokeFutureContext.future(String.class);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    System.err.println("Async result=" + result);
                } else {
                    throwable.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
