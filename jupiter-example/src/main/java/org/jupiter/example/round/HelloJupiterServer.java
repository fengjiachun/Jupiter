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
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.limiter.TpsLimiter;
import org.jupiter.rpc.provider.limiter.TpsResult;
import org.jupiter.transport.netty.NettyAcceptor;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

import java.util.concurrent.atomic.AtomicLong;

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
                    .tpsLimiter(new TpsLimiter<JRequest>() { // Provide级别限流器, 可以不设置

                        private final TpsResult CITY_WIDE_OPEN = new TpsResult(true);
                        private AtomicLong count = new AtomicLong();

                        @Override
                        public TpsResult checkTpsLimit(JRequest param) {
                            if (count.getAndIncrement() > 9999) {
                                return new TpsResult(false, "fuck out!!!");
                            }
                            return CITY_WIDE_OPEN;
                        }
                    })
                    .register();

//            server.setTpsLimiter(); // App级别限流器
            server.connectToConfigServer("127.0.0.1", 20001);
            server.publish(provider);
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
