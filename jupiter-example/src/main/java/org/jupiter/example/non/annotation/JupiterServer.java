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

package org.jupiter.example.non.annotation;

import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.example.ServiceNonAnnotationTest;
import org.jupiter.example.ServiceNonAnnotationTestImpl;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * jupiter
 * org.jupiter.example.non.annotation
 *
 * @author jiachun.fjc
 */
public class JupiterServer {

    public static void main(String[] args) {
        SystemPropertyUtil.setProperty("jupiter.message.args.allow_null_array_arg", "true");
        SystemPropertyUtil.setProperty("jupiter.serializer.protostuff.allow_null_array_element", "true");
        final JServer server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090));
        final MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            // provider1
            ServiceNonAnnotationTest service = new ServiceNonAnnotationTestImpl();

            ServiceWrapper provider = server.serviceRegistry()
                    .provider(service)
                    .interfaceClass(ServiceNonAnnotationTest.class)
                    .group("test")
                    .providerName("org.jupiter.example.ServiceNonAnnotationTest")
                    .version("1.0.0")
                    .register();

            server.connectToRegistryServer("127.0.0.1:20001");
            server.publish(provider);

            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    monitor.shutdownGracefully();
                    server.shutdownGracefully();
                }
            });

            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
