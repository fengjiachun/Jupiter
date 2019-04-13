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

import org.jupiter.common.util.Reflects;
import org.jupiter.flight.exec.UserExecInterface;
import org.jupiter.rpc.JServer;

/**
 * jupiter
 * org.jupiter.example.flight.exec
 *
 * @author jiachun.fjc
 */
public class UserExecImpl implements UserExecInterface {

    @Override
    public Object exec() {
        // System.out输出会返回客户端, 因为服务端执行前将该类的常量池修改了
        System.out.println("get server instance...");
        JServer[] servers = (JServer[]) Reflects.getStaticValue(FlightExecJupiterServer.class, "servers");
        System.out.println("server count=" + servers.length);

        for (JServer s : servers) {
            System.out.println(s.acceptor().localAddress() + " -----------------------");
            System.out.println(s.allRegisteredServices());
            System.out.println();
        }
        return "OK!";
    }
}
