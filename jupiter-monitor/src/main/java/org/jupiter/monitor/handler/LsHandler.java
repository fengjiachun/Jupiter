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

import java.util.Map;

import io.netty.channel.Channel;

import org.jupiter.common.util.JConstants;
import org.jupiter.monitor.Command;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.RegisterMeta.ServiceMeta;
import org.jupiter.registry.RegistryService;
import org.jupiter.registry.RegistryService.RegisterState;

/**
 * 本地查询发布和订阅的服务信息
 *
 * Jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class LsHandler implements CommandHandler {

    private volatile RegistryService serverRegisterService;
    private volatile RegistryService clientRegisterService;

    public RegistryService getServerRegisterService() {
        return serverRegisterService;
    }

    public void setServerRegisterService(RegistryService serverRegisterService) {
        this.serverRegisterService = serverRegisterService;
    }

    public RegistryService getClientRegisterService() {
        return clientRegisterService;
    }

    public void setClientRegisterService(RegistryService clientRegisterService) {
        this.clientRegisterService = clientRegisterService;
    }

    @Override
    public void handle(Channel channel, Command command, String... args) {
        if (AuthHandler.checkAuth(channel)) {
            // provider side
            if (serverRegisterService != null) {
                channel.writeAndFlush("Provider side: " + JConstants.NEWLINE);
                channel.writeAndFlush("--------------------------------------------------------------------------------"
                        + JConstants.NEWLINE);
                Map<RegisterMeta, RegisterState> providers = serverRegisterService.providers();
                for (Map.Entry<RegisterMeta, RegisterState> entry : providers.entrySet()) {
                    channel.writeAndFlush(entry.getKey() + " | " + entry.getValue().toString() + JConstants.NEWLINE);
                }
            }

            // consumer side
            if (clientRegisterService != null) {
                channel.writeAndFlush("Consumer side: " + JConstants.NEWLINE);
                channel.writeAndFlush("--------------------------------------------------------------------------------"
                        + JConstants.NEWLINE);
                Map<ServiceMeta, Integer> consumers = clientRegisterService.consumers();
                for (Map.Entry<ServiceMeta, Integer> entry : consumers.entrySet()) {
                    channel.writeAndFlush(entry.getKey() + " | address_size=" + entry.getValue() + JConstants.NEWLINE);
                }
            }
        }
    }
}
