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

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class HelpHandler implements CommandHandler {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        StringBuilder buf = new StringBuilder();
        buf.append("-- Help ------------------------------------------------------------------------")
                .append(JConstants.NEWLINE);
        for (Command parent : Command.values()) {
            buf.append(String.format("%1$-32s", parent.name().toLowerCase()))
                    .append(parent.description())
                    .append(JConstants.NEWLINE);

            for (Command.ChildCommand child : parent.children()) {
                buf.append(String.format("%1$36s", "-"))
                        .append(child.name().toLowerCase())
                        .append(' ')
                        .append(child.description())
                        .append(JConstants.NEWLINE);
            }

            buf.append(JConstants.NEWLINE);
        }
        channel.writeAndFlush(buf.toString());
    }
}
