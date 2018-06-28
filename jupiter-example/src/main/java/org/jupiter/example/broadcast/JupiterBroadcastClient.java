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
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.rpc.consumer.future.InvokeFutureGroup;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.UnresolvedSocketAddress;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * 广播调用客户端
 *
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class JupiterBroadcastClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());

        UnresolvedAddress[] addresses = {
                new UnresolvedSocketAddress("127.0.0.1", 18090),
                new UnresolvedSocketAddress("127.0.0.1", 18091),
                new UnresolvedSocketAddress("127.0.0.1", 18092),
                new UnresolvedSocketAddress("127.0.0.1", 18090),
                new UnresolvedSocketAddress("127.0.0.1", 18091),
                new UnresolvedSocketAddress("127.0.0.1", 18092),
                new UnresolvedSocketAddress("127.0.0.1", 18090)
        };

        for (UnresolvedAddress address : addresses) {
            client.connector().connect(address);
        }

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .dispatchType(DispatchType.BROADCAST)
                .invokeType(InvokeType.ASYNC)
                .addProviderAddress(addresses)
                .newProxyInstance();

        try {
            // callback方式
            System.out.println("callback-------------------------------");

            service.sayHello();

            InvokeFutureGroup<ServiceTest.ResultClass> futureGroup =
                    InvokeFutureContext.futureBroadcast(ServiceTest.ResultClass.class);
            futureGroup.addListener(new JListener<ServiceTest.ResultClass>() {

                @Override
                public void complete(ServiceTest.ResultClass result) {
                    System.out.print("Callback result: ");
                    System.out.println(result);
                }

                @Override
                public void failure(Throwable cause) {
                    cause.printStackTrace();
                }
            });

            // future.get方式
            System.out.println("future.get-------------------------------");

            service.sayHello();

            futureGroup =
                    InvokeFutureContext.futureBroadcast(ServiceTest.ResultClass.class);
            for (InvokeFuture<ServiceTest.ResultClass> f : futureGroup.futures()) {
                System.out.print("Async result: ");
                System.out.println(f.getResult());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
