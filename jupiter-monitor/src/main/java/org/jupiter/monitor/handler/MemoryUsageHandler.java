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
import org.jupiter.common.util.JvmTools;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.monitor.Command;

/**
 * Jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class MemoryUsageHandler implements CommandHandler {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        try {
            List<String> memoryUsageList = JvmTools.memoryUsage();
            for (String usage : memoryUsageList) {
                channel.writeAndFlush(usage);
            }
            channel.writeAndFlush(JConstants.NEWLINE);
        } catch (Exception e) {
            channel.writeAndFlush(StackTraceUtil.stackTrace(e));
        }
    }
}
