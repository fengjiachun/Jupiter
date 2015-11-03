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

import org.jupiter.example.ServiceTestImpl;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.NettyAcceptor;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterServer {

    public static void main(String[] args) {
        NettyAcceptor server = new JNettyTcpAcceptor(18090);
        MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            ServiceWrapper provider = server.serviceRegistry()
                    .provider(new ServiceTestImpl())
                    .register();

            server.initRegistryService("127.0.0.1", 20001);
            server.publish(provider, 18090);
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
