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

package org.jupiter.example.non.annotation;

import org.jupiter.common.util.Preconditions;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.example.ServiceNonAnnotationTest;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

import java.util.ArrayList;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class JupiterClient {

    public static void main(String[] args) {
        SystemPropertyUtil.setProperty("jupiter.message.args.allow_null_array_arg", "true");
        SystemPropertyUtil.setProperty("jupiter.serializer.protostuff.allow_null_array_element", "true");
        final JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(
                new ServiceMetadata("test", "org.jupiter.example.ServiceNonAnnotationTest", "1.0.0")
        );
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                client.shutdownGracefully();
            }
        });

        ServiceNonAnnotationTest service = ProxyFactory.factory(ServiceNonAnnotationTest.class)
                .group("test")
                .providerName("org.jupiter.example.ServiceNonAnnotationTest")
                .version("1.0.0")
                .client(client)
                .serializerType(SerializerType.PROTO_STUFF)
                .newProxyInstance();

        try {
            String result = service.sayHello(null, null, null);
            Preconditions.checkArgument("arg1=null, arg2=null, arg3=null".equals(result));
            System.out.println(result);
            result = service.sayHello(null, 1, null);
            Preconditions.checkArgument("arg1=null, arg2=1, arg3=null".equals(result));
            System.out.println(result);
            result = service.sayHello(null, null, new ArrayList<String>());
            Preconditions.checkArgument("arg1=null, arg2=null, arg3=[]".equals(result));
            System.out.println(result);
            result = service.sayHello("test", 2, null);
            Preconditions.checkArgument("arg1=test, arg2=2, arg3=null".equals(result));
            System.out.println(result);
            result = service.sayHello("test", null, new ArrayList<String>());
            Preconditions.checkArgument("arg1=test, arg2=null, arg3=[]".equals(result));
            System.out.println(result);
            result = service.sayHello(null, 3, new ArrayList<String>());
            Preconditions.checkArgument("arg1=null, arg2=3, arg3=[]".equals(result));
            System.out.println(result);
            result = service.sayHello("test2", 4, new ArrayList<String>());
            Preconditions.checkArgument("arg1=test2, arg2=4, arg3=[]".equals(result));
            System.out.println(result);
            result = service.sayHello2(new String[] { "a", null, "b" });
            Preconditions.checkArgument("[a, null, b]".equals(result));
            System.out.println(result);
            result = service.sayHello2(new String[] { null, "a", "b" });
            Preconditions.checkArgument("[null, a, b]".equals(result));
            System.out.println(result);
            result = service.sayHello2(new String[] { "a", "b", null });
            Preconditions.checkArgument("[a, b, null]".equals(result));
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
