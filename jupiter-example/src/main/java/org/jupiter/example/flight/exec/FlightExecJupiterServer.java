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
package org.jupiter.example.flight.exec;

import java.util.concurrent.CountDownLatch;

import org.jupiter.flight.exec.JavaClassExecProvider;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子.
 *
 * jupiter
 * org.jupiter.example.flight.exec
 *
 * @author jiachun.fjc
 */
public class FlightExecJupiterServer {

    private static JServer[] servers = {
            new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090)),
            new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18091))
    };

    public static void main(String[] args) {
        final CountDownLatch latch = new CountDownLatch(servers.length);
        for (final JServer server : servers) {
            new Thread(() -> {
                try {
                    ServiceWrapper service = server.serviceRegistry()
                            .provider(new JavaClassExecProvider())
                            .register();

                    server.connectToRegistryServer("127.0.0.1:20001");
                    server.publish(service);
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
