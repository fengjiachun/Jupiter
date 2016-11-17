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

import org.jupiter.common.util.Maps;
import org.jupiter.rpc.ConsumerHook;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;

import java.util.List;
import java.util.Map;

import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.serialization.SerializerHolder.serializerImpl;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public abstract class AbstractDispatcher implements Dispatcher {

    protected final ServiceMetadata metadata;       // 目标服务元信息
    protected final Serializer serializerImpl;      // 序列化/反序列化impl

    private ConsumerHook[] hooks;                   // consumer hook
    private long timeoutMillis = DEFAULT_TIMEOUT;   // 调用超时时间设置
    // 针对指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodsSpecialTimeoutMillis = Maps.newHashMap();

    public AbstractDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        this.metadata = metadata;
        this.serializerImpl = serializerImpl(serializerType.value());
    }

    @Override
    public ConsumerHook[] getHooks() {
        return hooks;
    }

    @Override
    public void setHooks(List<ConsumerHook> hooks) {
        if (!hooks.isEmpty()) {
            this.hooks = hooks.toArray(new ConsumerHook[hooks.size()]);
        }
    }

    @Override
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    @Override
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public long getMethodSpecialTimeoutMillis(String methodName) {
        Long methodSpecialTimeoutMillis = methodsSpecialTimeoutMillis.get(methodName);
        if (methodSpecialTimeoutMillis != null && methodSpecialTimeoutMillis > 0) {
            return methodSpecialTimeoutMillis;
        }
        return timeoutMillis;
    }

    @Override
    public void setMethodsSpecialTimeoutMillis(Map<String, Long> methodsSpecialTimeoutMillis) {
        this.methodsSpecialTimeoutMillis.putAll(methodsSpecialTimeoutMillis);
    }

    protected abstract InvokeFuture<?> asFuture(JChannel channel, JRequest request, Class<?> returnType, long timeoutMillis);
}
