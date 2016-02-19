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

import org.jupiter.example.ServiceTest;
import org.jupiter.example.ServiceTest2;
import org.jupiter.rpc.InvokeMode;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.invoker.PromiseInvoker;
import org.jupiter.rpc.consumer.promise.InvokeDone;
import org.jupiter.rpc.consumer.promise.InvokePipe;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

/**
 * 1.启动 HelloJupiterConfigServer
 * 2.再启动 HelloJupiterServer
 * 3.最后启动 HelloJupiterPromiseClient
 *
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterPromiseClient {

    public static void main(String[] args) {
        NettyConnector connector = new JNettyTcpConnector();
        // 连接ConfigServer
        connector.connectToConfigServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionManager manager1 = connector.manageConnections(ServiceTest.class);
        JConnector.ConnectionManager manager2 = connector.manageConnections(ServiceTest2.class);
        // 等待连接可用
        if (!manager1.waitForAvailable(3000) && !manager2.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        final ServiceTest service1 = ProxyFactory.factory(ServiceTest.class)
                .connector(connector)
                .invokeMode(InvokeMode.PROMISE)
                .newProxyInstance();
        final ServiceTest2 service2 = ProxyFactory.factory(ServiceTest2.class)
                .connector(connector)
                .invokeMode(InvokeMode.PROMISE)
                .newProxyInstance();

        try {
            service1.sayHello();
            PromiseInvoker.currentPromise()
                    .then(new InvokePipe() {

                        @Override
                        public void doInPipe(Object result) {
                            System.err.println("step1. service1.sayHello(): " + result);

                            service2.sayHelloString();
                        }
                    })
                    .then(new InvokeDone() {

                        @Override
                        public void onDone(Object result) {
                            System.err.println("step2. service2.sayHelloString(): " + result);
                        }
                    });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
