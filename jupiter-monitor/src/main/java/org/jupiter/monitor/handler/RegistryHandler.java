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
public class RegistryHandler implements CommandHandler {

    private volatile RegistryMonitor registryMonitor;

    public RegistryMonitor getRegistryMonitor() {
        return registryMonitor;
    }

    public void setRegistryMonitor(RegistryMonitor registryMonitor) {
        this.registryMonitor = registryMonitor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(Channel channel, Command command, String... args) {
        if (AuthHandler.checkAuth(channel)) {
            if (args.length < 3) {
                channel.writeAndFlush("Need more args!" + JConstants.NEWLINE);
                return;
            }

            Command.ChildCommand child = command.parseChild(args[1]);
            if (child != null) {
                CommandHandler childHandler = child.handler();
                if (childHandler == null) {
                    return;
                }
                if (childHandler instanceof ChildCommandHandler) {
                    if (((ChildCommandHandler) childHandler).getParent() == null) {
                        ((ChildCommandHandler) childHandler).setParent(this);
                    }
                }
                childHandler.handle(channel, command, args);
            } else {
                channel.writeAndFlush("Wrong args denied!" + JConstants.NEWLINE);
            }
        }
    }
}
