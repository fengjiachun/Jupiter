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
 * 组播调用客户端
 *
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class JupiterBroadcastClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().connector(new JNettyTcpConnector());

        UnresolvedAddress[] addresses = {
                new UnresolvedAddress("127.0.0.1", 18090),
                new UnresolvedAddress("127.0.0.1", 18091),
                new UnresolvedAddress("127.0.0.1", 18092),
                new UnresolvedAddress("127.0.0.1", 18090),
                new UnresolvedAddress("127.0.0.1", 18091),
                new UnresolvedAddress("127.0.0.1", 18092),
                new UnresolvedAddress("127.0.0.1", 18090)
        };

        for (UnresolvedAddress address : addresses) {
            client.connector().connect(address);
        }

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .dispatchType(BROADCAST)
                .invokeType(ASYNC)
                .addProviderAddress(addresses)
                .newProxyInstance();

        try {
            for (int i = 0; i < 10; i++) {
                System.out.println();
                System.out.println("try count[" + (i + 1) + "]----------------------------------------------------");

                ServiceTest.ResultClass result = service.sayHello();
                System.out.print("Sync result: ");
                System.out.println(result);

                InvokeFuture<ServiceTest.ResultClass>[] futures =
                        InvokeFutureContext.groupFutures(ServiceTest.ResultClass.class);
                for (InvokeFuture<ServiceTest.ResultClass> f : futures) {
                    System.out.print("Async result: ");
                    System.out.println(f.getResult());
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
