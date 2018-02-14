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

package org.jupiter.rpc.provider.processor;

import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.executor.CloseableExecutor;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.rpc.provider.processor.task.MessageTask;
import org.jupiter.transport.Directory;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.payload.JRequestPayload;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public class DefaultProviderProcessor extends AbstractProviderProcessor {

    private final JServer server;
    private final CloseableExecutor executor;

    public DefaultProviderProcessor(JServer server) {
        this(server, ProviderExecutors.executor());
    }

    public DefaultProviderProcessor(JServer server, CloseableExecutor executor) {
        this.server = server;
        this.executor = executor;
    }

    @Override
    public void handleRequest(JChannel channel, JRequestPayload requestPayload) throws Exception {
        MessageTask task = new MessageTask(this, channel, new JRequest(requestPayload));
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }

    @Override
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return server.lookupService(directory);
    }

    @Override
    public ControlResult flowControl(JRequest request) {
        // 全局流量控制
        FlowController<JRequest> controller = server.globalFlowController();
        if (controller == null) {
            return ControlResult.ALLOWED;
        }
        return controller.flowControl(request);
    }
}
