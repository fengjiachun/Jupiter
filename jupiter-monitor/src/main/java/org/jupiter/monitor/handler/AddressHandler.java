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
package org.jupiter.monitor.handler;

import java.util.List;

import io.netty.channel.Channel;

import org.jupiter.common.util.JConstants;
import org.jupiter.monitor.Command;
import org.jupiter.registry.RegistryMonitor;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class AddressHandler extends ChildCommandHandler<RegistryHandler> {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        RegistryMonitor monitor = getParent().getRegistryMonitor();
        if (monitor == null) {
            return;
        }

        Command.ChildCommand target = command.parseChild(args[2]);
        if (target == null) {
            channel.writeAndFlush("Wrong args denied!" + JConstants.NEWLINE);
            return;
        }

        List<String> addresses;
        switch (target) {
            case P:
                addresses = monitor.listPublisherHosts();

                break;
            case S:
                addresses = monitor.listSubscriberAddresses();

                break;
            default:
                return;
        }
        Command.ChildCommand childGrep = null;
        if (args.length >= 5) {
            childGrep = command.parseChild(args[3]);
        }
        for (String a : addresses) {
            if (childGrep == Command.ChildCommand.GREP) {
                if (a.contains(args[4])) {
                    channel.writeAndFlush(a + JConstants.NEWLINE);
                }
            } else {
                channel.writeAndFlush(a + JConstants.NEWLINE);
            }
        }
    }
}
