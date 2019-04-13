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
package org.jupiter.example.cluster;

import java.util.concurrent.CountDownLatch;

import org.jupiter.example.cluster.service.ClusterFailServiceImpl;
import org.jupiter.example.cluster.service.ClusterSuccessServiceImpl;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * jupiter
 * org.jupiter.example.cluster
 *
 * @author jiachun.fjc
 */
public class JupiterClusterServer {

    public static void main(String[] args) {
        // 启动5个server
        JServer[] servers = {
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090)),
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18091)),
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18092)),
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18093)),
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18094))
        };

        final CountDownLatch latch = new CountDownLatch(servers.length);
        for (int i = 0; i < servers.length; i++) {
            final JServer server = servers[i];

            final int index = i;
            new Thread(() -> {
                try {
                    JServer.ServiceRegistry registry = server.serviceRegistry(); // 获得本地registry
                    ServiceWrapper provider;
                    if (index > 3) {
                        provider = registry.provider(new ClusterSuccessServiceImpl())
                                .register(); // 注册provider到本地
                    } else {
                        // 模拟调用超时
                        provider = registry.provider(new ClusterFailServiceImpl())
                                .register(); // 注册provider到本地
                    }

                    server.connectToRegistryServer("127.0.0.1:20001");
                    server.publish(provider);

                    // server start后默认是block当前线程的
                    server.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
