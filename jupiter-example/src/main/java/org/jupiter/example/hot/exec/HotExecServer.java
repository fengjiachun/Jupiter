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

package org.jupiter.example.hot.exec;

import org.jupiter.hot.exec.JavaClassExecProvider;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.JNettyTcpAcceptor;
import org.jupiter.transport.netty.NettyAcceptor;

import java.util.concurrent.CountDownLatch;

/**
 * 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子.
 *
 * jupiter
 * org.jupiter.example.hot.exec
 *
 * @author jiachun.fjc
 */
public class HotExecServer {

    public static void main(String[] args) {
        NettyAcceptor acceptor1 = new JNettyTcpAcceptor(18090);
        NettyAcceptor acceptor2 = new JNettyTcpAcceptor(18091);

        NettyAcceptor[] servers = { acceptor1, acceptor2 };
        final CountDownLatch latch = new CountDownLatch(servers.length);
        for (final NettyAcceptor acceptor : servers) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ServiceWrapper service = acceptor.serviceRegistry()
                                .provider(new JavaClassExecProvider())
                                .register();

                        acceptor.connectToConfigServer("127.0.0.1", 20001);
                        acceptor.publish(service);
                        acceptor.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
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
