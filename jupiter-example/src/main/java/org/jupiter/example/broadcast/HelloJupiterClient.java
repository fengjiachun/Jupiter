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

package org.jupiter.example.broadcast;

import org.jupiter.example.ServiceTest;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.netty.JNettyTcpConnector;

import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.InvokeType.CALLBACK;

/**
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class HelloJupiterClient {

    public static void main(String[] args) {
        JConnector<JConnection> connector = new JNettyTcpConnector();
        UnresolvedAddress address1 = new UnresolvedAddress("127.0.0.1", 18090);
        UnresolvedAddress address2 = new UnresolvedAddress("127.0.0.1", 18091);
        UnresolvedAddress address3 = new UnresolvedAddress("127.0.0.1", 18090);
        connector.connect(address1);
        connector.connect(address2);
        connector.connect(address3);

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .connector(connector)
                .dispatchType(BROADCAST)
                .invokeType(CALLBACK)
                .addProviderAddress(address1, address2, address2)
                .listener(new JListener() {

                    @Override
                    public void complete(JRequest request, JResult result) throws Exception {
                        System.out.println("complete=" + result);
                    }

                    @Override
                    public void failure(JRequest request, Throwable cause) {
                        System.out.println("failure=" + cause);
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
