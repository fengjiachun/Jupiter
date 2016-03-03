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

import org.jupiter.example.ServiceTest2Impl;
import org.jupiter.example.ServiceTestImpl;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.rpc.provider.ProviderProxyHandler;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.transport.netty.JNettyTcpAcceptor;
import org.jupiter.transport.netty.NettyAcceptor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 1.启动 HelloJupiterConfigServer
 * 2.再启动 HelloJupiterServer
 * 3.最后启动 HelloJupiterClient / HelloJupiterPromiseClient / HelloJupiterCallbackClient
 *
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

            // provider的通用interceptor, 可不设置
            ProviderProxyHandler proxyHandler = new ProviderProxyHandler();
            proxyHandler.addProviderInterceptor(new ProviderInterceptor() {

                @Override
                public void before(TraceId traceId, String methodName, Object[] args) {
                    System.out.println("before: " + methodName + " args: " + Arrays.toString(args));
                }

                @Override
                public void after(TraceId traceId, String methodName, Object[] args, Object result) {
                    System.out.println("after: " + methodName + " args: " + Arrays.toString(args) + " result: " + result);
                }
            });
            server.setProviderProxyHandler(proxyHandler);

            // provider1
            ServiceWrapper provider1 = server.serviceRegistry()
                    .provider(new ServiceTestImpl())
                    .register();

            // provider2
            ServiceWrapper provider2 = server.serviceRegistry()
                    .provider(new ServiceTest2Impl())
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
            server.connectToConfigServer("127.0.0.1:20001");
            server.publish(provider1, provider2);
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
