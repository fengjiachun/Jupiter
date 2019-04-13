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
package org.jupiter.example.generic;

import java.util.concurrent.atomic.AtomicLong;

import org.jupiter.example.GenericServiceTestImpl;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * jupiter
 * org.jupiter.example.generic
 *
 * @author jiachun.fjc
 */
public class GenericJupiterServer {

    public static void main(String[] args) {
        JServer server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090));
        MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            ServiceWrapper provider = server.serviceRegistry()
                    .provider(new GenericServiceTestImpl())
                    .flowController(new FlowController<JRequest>() { // provider级别限流器, 可不设置

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

//            server.withGlobalFlowController(); // 全局限流器
            server.connectToRegistryServer("127.0.0.1:20001");
            server.publish(provider);
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
