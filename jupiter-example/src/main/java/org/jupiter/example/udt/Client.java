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

package org.jupiter.example.udt;

import org.jupiter.example.ServiceTest;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.netty.JNettyUdtConnector;

/**
 * jupiter
 * org.jupiter.example.udt
 *
 * @author jiachun.fjc
 */
public class Client {

    public static void main(String[] args) {
        JClient client = new DefaultClient().connector(new JNettyUdtConnector());
        UnresolvedAddress address = new UnresolvedAddress("127.0.0.1", 18090);
        client.connector().connect(address);

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0.daily")
                .client(client)
                .addProviderAddress(address)
                .newProxyInstance();

        ServiceTest.ResultClass result = service.sayHello();
        System.out.println(result);
    }
}
