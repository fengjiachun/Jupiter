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

package org.jupiter.example.hot.exec;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.hot.exec.ExecResult;
import org.jupiter.hot.exec.JavaClassExec;
import org.jupiter.hot.exec.JavaCompiler;
import org.jupiter.rpc.Directory;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.error.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

import static org.jupiter.common.util.JConstants.DEFAULT_VERSION;
import static org.jupiter.rpc.AsyncMode.ASYNC_CALLBACK;
import static org.jupiter.rpc.DispatchMode.BROADCAST;

/**
 * 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子.
 *
 * jupiter
 * org.jupiter.example.hot.exec
 *
 * @author jiachun.fjc
 */
public class HotExecClient {

    public static void main(String[] args) {
        Directory directory = new ServiceMetadata("exec", DEFAULT_VERSION, "JavaClassExec");

        NettyConnector connector = new JNettyTcpConnector();
        // 连接ConfigServer
        connector.connectToConfigServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionManager manager = connector.manageConnections(directory);
        // 等待连接可用
        if (!manager.waitForAvailable(3000)) {
            throw new ConnectFailedException("waitForAvailable() timeout");
        }

        JavaClassExec service = ProxyFactory.factory(JavaClassExec.class)
                .connector(connector)
                .dispatchMode(BROADCAST)
                .asyncMode(ASYNC_CALLBACK)
                .listener(new JListener() {

                    @Override
                    public void complete(JRequest request, JResult result) throws Exception {
                        synchronized (this) {
                            System.out.println("complete from " + result.remoteAddress());
                            ExecResult execResult = (ExecResult) result.value();
                            System.out.println("= debug info ======================================");
                            System.out.println(execResult.getDebugInfo());
                            System.out.println("= return value ====================================");
                            System.out.println(execResult.getValue());
                            System.out.println();
                            System.out.println();
                        }
                    }

                    @Override
                    public void failure(JRequest request, Throwable cause) {
                        System.out.println("failure=" + cause);
                    }
                })
                .newProxyInstance();

        try {
            byte[] classBytes = JavaCompiler.compile(
                    SystemPropertyUtil.get("user.dir") + "/jupiter-example/src/main/java/",
                    UserExecImpl.class.getName(),
                    Lists.newArrayList("-verbose", "-source", "1.7", "-target", "1.7"));

            service.exec(classBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
