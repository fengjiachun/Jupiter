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
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

/**
 * 1.启动 HelloJupiterRegistryServer
 * 2.再启动 HelloJupiterServer
 * 3.最后启动 HelloJupiterCallbackClient
 *
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterCallbackClient {

    public static void main(String[] args) {
        NettyConnector connector = new JNettyTcpConnector();
        // 连接RegistryServer
        connector.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionManager manager = connector.manageConnections(ServiceTest.class);
        // 等待连接可用
        if (!manager.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .connector(connector)
                .invokeType(InvokeType.CALLBACK)
                .listener(new JListener() {
                    @Override
                    public void complete(JRequest request, JResult result) throws Exception {
                        System.out.println("complete: " + result);
                    }

                    @Override
                    public void failure(JRequest request, Throwable cause) {
                        System.out.println("failure: " + cause);
                    }
                })
                .newProxyInstance();

        try {
            ServiceTest.ResultClass result = service.sayHello();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
