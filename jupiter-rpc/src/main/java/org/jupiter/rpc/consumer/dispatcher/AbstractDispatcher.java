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

import org.jupiter.common.concurrent.atomic.AtomicUpdater;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.channel.CopyOnWriteGroupList;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.channel.JFutureListener;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.load.balance.LoadBalancer;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingRecorder;
import org.jupiter.rpc.tracing.TracingUtil;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.rpc.Status.CLIENT_ERROR;
import static org.jupiter.rpc.tracing.TracingRecorder.Role.CONSUMER;
import static org.jupiter.serialization.SerializerHolder.serializerImpl;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public abstract class AbstractDispatcher implements Dispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractDispatcher.class);

    private static final AtomicReferenceFieldUpdater<CopyOnWriteGroupList, Object[]> groupsUpdater
            = AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteGroupList.class, Object[].class, "array");

    private final LoadBalancer<JChannelGroup> loadBalancer;
    private final ServiceMetadata metadata;         // 目标服务元信息
    private final Serializer serializerImpl;        // 序列化/反序列化impl
    private ConsumerHook[] hooks;                   // consumer hook
    private long timeoutMillis = DEFAULT_TIMEOUT;   // 调用超时时间设置
    // 针对指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodsSpecialTimeoutMillis = Maps.newHashMap();

    public AbstractDispatcher(LoadBalancer<JChannelGroup> loadBalancer, ServiceMetadata metadata, SerializerType serializerType) {
        this.loadBalancer = loadBalancer;
        this.metadata = metadata;
        this.serializerImpl = serializerImpl(serializerType.value());
    }

    @Override
    public Object dispatch(JClient client, JChannel channel, String methodName, Object[] args, Class<?> returnType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JChannel select(JClient client, MessageWrapper message) {
        // stack copy
        final Directory directory = message.getMetadata();

        CopyOnWriteGroupList groups = client.directory(directory);
        // snapshot of groupList
        Object[] elements = groupsUpdater.get(groups);
        if (elements.length == 0) {
            if (!client.awaitConnections(directory, 3000)) {
                throw new IllegalStateException("no connections");
            }
            elements = groupsUpdater.get(groups);
        }

        JChannelGroup group = loadBalancer.select(elements, message);

        if (group.isAvailable()) {
            return group.next();
        }

        client.refreshConnections(directory);

        // group死期到(无可用channel), 时间超过预定限制
        long deadline = group.deadlineMillis();
        if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
            boolean removed = groups.remove(group);
            if (removed) {
                logger.warn("Removed channel group: {} in directory: {} on [select].", group, directory.directory());
            }
        }

        for (int i = 0; i < groups.size(); i++) {
            JChannelGroup g = groups.get(i);
            if (g.isAvailable()) {
                return g.next();
            }
        }

        throw new IllegalStateException("no channel");
    }

    @Override
    public ServiceMetadata getMetadata() {
        return metadata;
    }

    @Override
    public Serializer getSerializer() {
        return serializerImpl;
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

    // Tracing
    protected MessageWrapper doTracing(JRequest request, MessageWrapper message, String methodName, JChannel channel) {
        if (TracingUtil.isTracingNeeded()) {
            TraceId traceId = TracingUtil.getCurrent();
            if (traceId == null) {
                traceId = TraceId.newInstance(TracingUtil.generateTraceId());
            }
            message.setTraceId(traceId);

            TracingRecorder recorder = TracingUtil.getRecorder();
            recorder.recording(CONSUMER, traceId.asText(), request.invokeId(), metadata.directory(), methodName, channel);
        }
        return message;
    }

    protected InvokeFuture<?> write(JChannel channel, final JRequest request, final InvokeFuture<?> future) {
        channel.write(request, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                future.setSentTime(); // 记录发送时间戳

                // hook.before()
                ConsumerHook[] _hooks = future.getHooks();
                if (_hooks != null) {
                    for (ConsumerHook h : _hooks) {
                        h.before(request, channel);
                    }
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                logger.warn("Writes {} fail on {}, {}.", request, channel, cause);

                ResultWrapper result = new ResultWrapper();
                result.setError(cause);

                InvokeFuture.received(
                        channel,
                        JResponse.newInstance(
                                request.invokeId(),
                                request.serializerCode(),
                                CLIENT_ERROR,
                                result
                        )
                );
            }
        });

        return future;
    }

    protected abstract InvokeFuture<?> asFuture(JChannel channel, JRequest request, Class<?> returnType, long timeoutMillis);
}
