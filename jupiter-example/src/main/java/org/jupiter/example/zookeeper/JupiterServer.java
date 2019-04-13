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

import java.util.concurrent.atomic.AtomicLong;

import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.example.ServiceTestImpl;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * RegistryServer是基于SPI的, 使用zookeeper的话maven引入jupiter-registry-zookeeper即可
 *
 * jupiter
 * org.jupiter.example.zookeeper
 *
 * @author jiachun.fjc
 */
public class JupiterServer {

    public static void main(String[] args) {
        try {
            Class.forName("org.jupiter.registry.zookeeper.ZookeeperRegistryService");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("RegistryServer是基于SPI的, 使用zookeeper的话maven引入jupiter-registry-zookeeper即可");
            return;
        }

        SystemPropertyUtil.setProperty("jupiter.local.address", "127.0.0.1");

        final JServer server = new DefaultServer(RegistryService.RegistryType.ZOOKEEPER)
                .withAcceptor(new JNettyTcpAcceptor(18090));
        MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            ServiceWrapper provider = server.serviceRegistry()
                    .provider(new ServiceTestImpl())
                    .weight(60)
                    .flowController(new FlowController<JRequest>() { // provider级别限流器, 可以不设置

                        private AtomicLong count = new AtomicLong();

                        @Override
                        public ControlResult flowControl(JRequest request) {
                            if (count.getAndIncrement() > 9999) {
                                return new ControlResult(false, "go go go!!!");
                            }
                            return ControlResult.ALLOWED;
                        }
                    })
                    .register();

            server.connectToRegistryServer("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183");
            server.publish(provider);

            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownGracefully));

            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
