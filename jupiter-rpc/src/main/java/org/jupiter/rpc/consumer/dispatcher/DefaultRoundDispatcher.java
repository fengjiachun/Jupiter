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

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.aop.ConsumerHook;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.serialization.SerializerHolder;

import java.util.List;

/**
 * 单播派发
 *
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public class DefaultRoundDispatcher extends AbstractDispatcher {

    public DefaultRoundDispatcher(JClient connector) {
        super(connector);
    }

    @Override
    public InvokeFuture dispatch(MessageWrapper message) {
        JChannel jChannel = connector.select(message);

        final Request request = new Request();
        request.message(message);
        // 在业务线程里序列化, 减轻IO线程负担
        request.bytes(SerializerHolder.serializer().writeObject(message));
        final List<ConsumerHook> _hooks = getHooks();
        final InvokeFuture invokeFuture = new DefaultInvokeFuture(jChannel, request, getTimeoutMills())
                .hooks(_hooks)
                .listener(getListener());

        jChannel.write(request, new JFutureListener<JChannel>() {

            @Override
            public void operationComplete(JChannel ch, boolean isSuccess) throws Exception {
                if (isSuccess) {
                    invokeFuture.sent();

                    if (_hooks != null) {
                        for (ConsumerHook h : _hooks) {
                            h.before(request);
                        }
                    }
                }
            }
        });

        return invokeFuture;
    }
}
