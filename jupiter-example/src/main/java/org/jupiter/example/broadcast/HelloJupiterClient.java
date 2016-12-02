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
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.netty.JNettyTcpConnector;

import static org.jupiter.rpc.DispatchType.BROADCAST;
import static org.jupiter.rpc.InvokeType.ASYNC;

/**
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class HelloJupiterClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().connector(new JNettyTcpConnector());
        UnresolvedAddress address1 = new UnresolvedAddress("127.0.0.1", 18090);
        UnresolvedAddress address2 = new UnresolvedAddress("127.0.0.1", 18091);
        UnresolvedAddress address3 = new UnresolvedAddress("127.0.0.1", 18090);
        client.connector().connect(address1);
        client.connector().connect(address2);
        client.connector().connect(address3);

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .dispatchType(BROADCAST)
                .invokeType(ASYNC)
                .addProviderAddress(address1, address2, address2)
                .newProxyInstance();

        try {
            for (int i = 0; i < 10; i++) {
                ServiceTest.ResultClass result = service.sayHello();
                System.out.println(result);

                InvokeFuture<ServiceTest.ResultClass>[] futures = InvokeFutureContext.futures(ServiceTest.ResultClass.class);
                for (InvokeFuture<ServiceTest.ResultClass> f : futures) {
                    System.out.println(f.getResult());
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
