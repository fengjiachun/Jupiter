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
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.load.balance.LoadBalancer;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingRecorder;
import org.jupiter.rpc.tracing.TracingUtil;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JChannelGroup;
import org.jupiter.transport.channel.JFutureListener;
import org.jupiter.transport.payload.JRequestBytes;

import java.util.List;
import java.util.Map;

import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;
import static org.jupiter.rpc.ConsumerHook.EMPTY_HOOKS;
import static org.jupiter.rpc.DispatchType.ROUND;
import static org.jupiter.rpc.tracing.TracingRecorder.Role.CONSUMER;
import static org.jupiter.transport.Status.CLIENT_ERROR;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public abstract class AbstractDispatcher implements Dispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractDispatcher.class);

    private final LoadBalancer loadBalancer;        // 软负载均衡
    private final ServiceMetadata metadata;         // 目标服务元信息
    private final Serializer serializerImpl;        // 序列化/反序列化impl
    private ConsumerHook[] hooks = EMPTY_HOOKS;     // 消费者端钩子函数
    private long timeoutMillis = DEFAULT_TIMEOUT;   // 调用超时时间设置
    // 针对指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodsSpecialTimeoutMillis = Maps.newHashMap();

    public AbstractDispatcher(LoadBalancer loadBalancer, ServiceMetadata metadata, SerializerType serializerType) {
        this.loadBalancer = loadBalancer;
        this.metadata = metadata;
        this.serializerImpl = SerializerFactory.getSerializer(serializerType.value());
    }

    @SuppressWarnings("all")
    @Override
    public JChannel select(JClient client, MessageWrapper message) {
        // stack copy
        final ServiceMetadata _metadata = metadata;

        CopyOnWriteGroupList groups = client.connector().directory(_metadata);
        JChannelGroup group = loadBalancer.select(groups, message);

        if (group != null) {
            if (group.isAvailable()) {
                return group.next();
            }

            // group死期到(无可用channel), 时间超过预定限制
            long deadline = group.deadlineMillis();
            if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
                boolean removed = groups.remove(group);
                if (removed) {
                    logger.warn("Removed channel group: {} in directory: {} on [select].", group, _metadata.directory());
                }
            }
        } else {
            if (!client.awaitConnections(_metadata, 3000)) {
                throw new IllegalStateException("no connections");
            }
        }

        Object[] snapshot = groups.snapshot();
        for (int i = 0; i < snapshot.length; i++) {
            JChannelGroup g = (JChannelGroup) snapshot[i];
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

    protected DefaultInvokeFuture<?> write(
            JChannel channel, final JRequest request, final DefaultInvokeFuture<?> future, final DispatchType dispatchType) {

        final JRequestBytes requestBytes = request.requestBytes();
        final ConsumerHook[] hooks = future.hooks();

        channel.write(requestBytes, new JFutureListener<JChannel>() {

            @SuppressWarnings("all")
            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                future.markSent(); // 标记已发送

                if (ROUND == dispatchType) {
                    requestBytes.nullBytes();
                }

                // hook.before()
                for (int i = 0; i < hooks.length; i++) {
                    hooks[i].before(request, channel);
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                if (ROUND == dispatchType) {
                    requestBytes.nullBytes();
                }

                logger.warn("Writes {} fail on {}, {}.", request, channel, cause);

                ResultWrapper result = new ResultWrapper();
                result.setError(cause);

                JResponse response = new JResponse(requestBytes.invokeId());
                response.status(CLIENT_ERROR);
                response.result(result);

                DefaultInvokeFuture.received(channel, response);
            }
        });

        return future;
    }

    protected abstract DefaultInvokeFuture<?> asFuture(JRequest request, JChannel channel, Class<?> returnType, long timeoutMillis);
}
