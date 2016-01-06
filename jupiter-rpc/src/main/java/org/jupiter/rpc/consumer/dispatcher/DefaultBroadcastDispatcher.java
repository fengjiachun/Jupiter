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

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.aop.ConsumerHook;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;

import java.util.List;

import static org.jupiter.rpc.DispatchMode.BROADCAST;
import static org.jupiter.serialization.SerializerHolder.serializer;

/**
 * 组播方式派发消息
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultBroadcastDispatcher extends AbstractDispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultBroadcastDispatcher.class);

    public DefaultBroadcastDispatcher(ServiceMetadata metadata) {
        super(metadata);
    }

    @Override
    public InvokeFuture dispatch(JClient proxy, String method, Object[] args) {
        // stack copy
        final ServiceMetadata _metadata = metadata;

        MessageWrapper message = new MessageWrapper(_metadata);
        message.setAppName(proxy.appName());
        message.setMethodName(method);
        message.setArgs(args);

        List<JChannelGroup> groupList = proxy.directory(_metadata);
        List<JChannel> channels = Lists.newArrayListWithCapacity(groupList.size());
        for (JChannelGroup group : groupList) {
            if (group.isAvailable()) {
                channels.add(group.next());
            }
        }

        final JRequest request = new JRequest();
        request.message(message);
        // 在非IO线程里序列化, 减轻IO线程负担
        request.bytes(serializer().writeObject(message));
        final List<ConsumerHook> _hooks = getHooks();
        final JListener _listener = getListener();
        for (JChannel ch : channels) {
            final InvokeFuture invokeFuture = new DefaultInvokeFuture(ch, request, getTimeoutMills(), BROADCAST)
                    .hooks(_hooks)
                    .listener(_listener);

            ch.write(request, new JFutureListener<JChannel>() {

                @Override
                public void operationComplete(JChannel channel, boolean isSuccess) throws Exception {
                    if (isSuccess) {
                        invokeFuture.sent();

                        if (_hooks != null) {
                            for (ConsumerHook h : _hooks) {
                                h.before(request);
                            }
                        }
                    } else {
                        logger.warn("Writes {} fail on {}.", request, channel);
                    }
                }
            });
        }

        return null;
    }
}
