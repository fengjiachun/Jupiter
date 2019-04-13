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
package org.jupiter.example.broadcast;

import java.util.concurrent.CountDownLatch;

import org.jupiter.example.ServiceTestImpl;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JServer;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * 广播调用服务端
 *
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class JupiterBroadcastServer {

    public static void main(String[] args) {
        // 启动3个server
        JServer[] servers = {
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090)),
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18091)),
                new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18092))
        };

        final CountDownLatch latch = new CountDownLatch(servers.length);
        for (final JServer server : servers) {
            new Thread(() -> {
                try {
                    server.serviceRegistry() // 获得本地registry
                            .provider(new ServiceTestImpl())
                            .register(); // 注册provider到本地

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
