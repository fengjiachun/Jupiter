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
import org.jupiter.monitor.metric.MetricsReporter;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class MetricsHandler implements CommandHandler {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        if (AuthHandler.checkAuth(channel)) {
            if (args.length < 2) {
                channel.writeAndFlush("Need second arg!" + JConstants.NEWLINE);
                return;
            }

            Command.ChildCommand child = command.parseChild(args[1]);
            if (child != null) {
                if (child == Command.ChildCommand.REPORT) {
                    channel.writeAndFlush(MetricsReporter.report());
                }
            } else {
                channel.writeAndFlush("Wrong args denied!" + JConstants.NEWLINE);
            }
        }
    }
}
