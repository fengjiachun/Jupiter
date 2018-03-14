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

import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.example.UserServiceImpl;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.JOption;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class JupiterServer {

    static {
        SystemPropertyUtil.setProperty("jupiter.rpc.suggest.connection.count", "1");
        SystemPropertyUtil.setProperty("jupiter.transport.codec.low_copy", "true");
    }

    public static void main(String[] args) {
        JServer server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090));
        server.acceptor().configGroup().child().setOption(JOption.PREFER_DIRECT, false);
        try {
            ServiceWrapper provider = server.serviceRegistry()
                    .provider(new UserServiceImpl())
                    .register();

            server.connectToRegistryServer("127.0.0.1:20001");
            server.publish(provider);
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
