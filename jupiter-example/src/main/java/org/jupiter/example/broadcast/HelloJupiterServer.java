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

import org.jupiter.example.ServiceTestImpl;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.NettyAcceptor;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class HelloJupiterServer {

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
                        ServiceWrapper provider = acceptor.serviceRegistry()
                                .provider(new ServiceTestImpl())
                                .register();

                        int port = ((InetSocketAddress) acceptor.localAddress()).getPort();
                        acceptor.publish(provider, port);
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
