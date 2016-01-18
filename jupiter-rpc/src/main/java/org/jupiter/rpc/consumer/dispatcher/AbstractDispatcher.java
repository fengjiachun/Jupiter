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

package org.jupiter.rpc.consumer.dispatcher;

import org.jupiter.rpc.ConsumerHook;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import java.util.List;

import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public abstract class AbstractDispatcher implements Dispatcher {

    protected final ServiceMetadata metadata;

    private ConsumerHook[] hooks;
    private JListener listener;
    private int timeoutMills = DEFAULT_TIMEOUT;

    public AbstractDispatcher(ServiceMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public ConsumerHook[] getHooks() {
        return hooks;
    }

    @Override
    public void setHooks(List<ConsumerHook> hooks) {
        this.hooks = hooks.toArray(new ConsumerHook[hooks.size()]);
    }

    @Override
    public JListener getListener() {
        return listener;
    }

    @Override
    public void setListener(JListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTimeoutMills() {
        return timeoutMills;
    }

    @Override
    public void setTimeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
    }

    protected abstract InvokeFuture asInvokeFuture(JChannel channel, JRequest request);
}
