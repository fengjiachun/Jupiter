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

import org.jupiter.example.ServiceTestImpl;
import org.jupiter.transport.netty.JNettyUdtAcceptor;
import org.jupiter.transport.netty.NettyAcceptor;

/**
 * jupiter
 * org.jupiter.example.udt
 *
 * @author jiachun.fjc
 */
public class Server {

    public static void main(String[] args) {
        NettyAcceptor server = new JNettyUdtAcceptor(18090);
        try {
            server.serviceRegistry()
                    .provider(new ServiceTestImpl())
                    .register();

            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
