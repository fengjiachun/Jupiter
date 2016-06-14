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

package org.jupiter.example.zookeeper;

import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.example.ServiceTestImpl;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.JAcceptor;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RegistryServer是基于SPI的, 使用zookeeper的话maven引入jupiter-registry-zookeeper即可
 *
 * jupiter
 * org.jupiter.example.zookeeper
 *
 * @author jiachun.fjc
 */
public class HelloJupiterServer {

    public static void main(String[] args) {
        try {
            Class.forName("org.jupiter.registry.zookeeper.ZookeeperRegistryService");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("RegistryServer是基于SPI的, 使用zookeeper的话maven引入jupiter-registry-zookeeper即可");
            return;
        }

        SystemPropertyUtil.setProperty("jupiter.address", "127.0.0.1");

        final JAcceptor acceptor = new JNettyTcpAcceptor(18090);
        MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            ServiceWrapper provider = acceptor.serviceRegistry()
                    .provider(new ServiceTestImpl())
                    .weight(60)
                    .connCount(1)
                    .flowController(new FlowController<JRequest>() { // Provider级别限流器, 可以不设置

                        private AtomicLong count = new AtomicLong();

                        @Override
                        public ControlResult flowControl(JRequest request) {
                            if (count.getAndIncrement() > 9999) {
                                return new ControlResult(false, "fuck out!!!");
                            }
                            return ControlResult.ALLOWED;
                        }
                    })
                    .register();

//            server.setFlowController(); // App级别限流器
            acceptor.connectToRegistryServer("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183");
            acceptor.publish(provider);

            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    acceptor.unpublishAll();
                    acceptor.shutdownGracefully();
                }
            });

            acceptor.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
