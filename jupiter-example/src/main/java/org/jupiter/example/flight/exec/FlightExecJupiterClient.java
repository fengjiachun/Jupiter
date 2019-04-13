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
package org.jupiter.example.flight.exec;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.flight.exec.ExecResult;
import org.jupiter.flight.exec.JavaClassExec;
import org.jupiter.flight.exec.JavaCompiler;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.rpc.consumer.future.InvokeFutureGroup;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子.
 *
 * jupiter
 * org.jupiter.example.flight.exec
 *
 * @author jiachun.fjc
 */
public class FlightExecJupiterClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(JavaClassExec.class);
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException("waitForAvailable() timeout");
        }

        JavaClassExec service = ProxyFactory.factory(JavaClassExec.class)
                .version("1.0.0")
                .client(client)
                .dispatchType(DispatchType.BROADCAST)
                .invokeType(InvokeType.ASYNC)
                .newProxyInstance();

        try {
            byte[] classBytes = JavaCompiler.compile(
                    SystemPropertyUtil.get("user.dir") + "/jupiter-example/src/main/java/",
                    UserExecImpl.class.getName(),
                    Lists.newArrayList("-verbose", "-source", "1.7", "-target", "1.7"));

            service.exec(classBytes);

            final InvokeFutureGroup<ExecResult> future = InvokeFutureContext.futureBroadcast(ExecResult.class);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    synchronized (future) {
                        System.out.println("= debug info ======================================");
                        System.out.println(result.getDebugInfo());
                        System.out.println("= return value ====================================");
                        System.out.println(result.getValue());
                        System.out.println();
                        System.out.println();
                    }
                } else {
                    throwable.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
