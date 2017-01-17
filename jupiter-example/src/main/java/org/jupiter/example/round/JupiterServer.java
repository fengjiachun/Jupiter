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
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.ProviderInterceptor;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class JupiterServer {

    public static void main(String[] args) {
        final JServer server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090));
        MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            server.withInterceptors(new GlobalInterceptor());

            // provider1
            ServiceTestImpl service = new ServiceTestImpl();

            ServiceWrapper provider1 = server.serviceRegistry()
                    .provider(service, new PrivateInterceptor())
                    .register();

            // provider2
            ServiceWrapper provider2 = server.serviceRegistry()
                    .provider(new ServiceTest2Impl())
                    .flowController(new PrivateFlowController()) // provider级别限流器, 可不设置
                    .register();

//            server.withGlobalFlowController(); // 全局限流器
            server.connectToRegistryServer("127.0.0.1:20001");
            server.publishWithInitializer(provider1, new JServer.ProviderInitializer<ServiceTestImpl>() {

                @Override
                public void init(ServiceTestImpl provider) {
                    // 初始化成功后再发布服务
                    provider.setStrValue("provider1");
                    provider.setIntValue(111);
                }
            });
            server.publish(provider2);

            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    server.unpublishAll();
                    server.shutdownGracefully();
                }
            });

            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class PrivateFlowController implements FlowController<JRequest> {

        private AtomicLong count = new AtomicLong();

        @Override
        public ControlResult flowControl(JRequest request) {
            if (count.getAndIncrement() > 9999) {
                return new ControlResult(false, "fuck out!!!");
            }
            return ControlResult.ALLOWED;
        }
    }

    static class GlobalInterceptor implements ProviderInterceptor {

        @Override
        public void beforeInvoke(TraceId traceId, Object provider, String methodName, Object[] args) {
            System.out.println("GlobalInterceptor before: " + provider + "#" + methodName + " args: " + Arrays.toString(args));
        }

        @Override
        public void afterInvoke(TraceId traceId, Object provider, String methodName, Object[] args, Object result) {
            System.out.println("GlobalInterceptor after: " + provider + "#" + methodName + " args: " + Arrays.toString(args) + " result: " + result);
        }
    }

    static class PrivateInterceptor implements ProviderInterceptor {

        @Override
        public void beforeInvoke(TraceId traceId, Object provider, String methodName, Object[] args) {
            System.out.println("PrivateInterceptor before: " + provider + "#" + methodName + " args: " + Arrays.toString(args));
        }

        @Override
        public void afterInvoke(TraceId traceId, Object provider, String methodName, Object[] args, Object result) {
            System.out.println("PrivateInterceptor after: " + provider + "#" + methodName + " args: " + Arrays.toString(args) + " result: " + result);
        }
    }
}
