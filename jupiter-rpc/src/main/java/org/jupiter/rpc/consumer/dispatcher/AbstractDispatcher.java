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

import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.exception.JupiterRemoteException;
import org.jupiter.rpc.load.balance.LoadBalancer;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingRecorder;
import org.jupiter.rpc.tracing.TracingUtil;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.Status;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JChannelGroup;
import org.jupiter.transport.channel.JFutureListener;
import org.jupiter.transport.payload.JRequestBytes;

import java.util.List;
import java.util.Map;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
abstract class AbstractDispatcher implements Dispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractDispatcher.class);

    private final LoadBalancer loadBalancer;                    // 软负载均衡
    private final ServiceMetadata metadata;                     // 目标服务元信息
    private final Serializer serializerImpl;                    // 序列化/反序列化impl
    private ConsumerHook[] hooks = ConsumerHook.EMPTY_HOOKS;    // 消费者端钩子函数
    private long timeoutMillis = JConstants.DEFAULT_TIMEOUT;    // 调用超时时间设置
    // 针对指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodSpecialTimeoutMapping = Maps.newHashMap();

    public AbstractDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        this(null, metadata, serializerType);
    }

    public AbstractDispatcher(LoadBalancer loadBalancer, ServiceMetadata metadata, SerializerType serializerType) {
        this.loadBalancer = loadBalancer;
        this.metadata = metadata;
        this.serializerImpl = SerializerFactory.getSerializer(serializerType.value());
    }

    @Override
    public ServiceMetadata metadata() {
        return metadata;
    }

    public Serializer serializer() {
        return serializerImpl;
    }

    public ConsumerHook[] hooks() {
        return hooks;
    }

    @Override
    public Dispatcher hooks(List<ConsumerHook> hooks) {
        if (hooks != null && !hooks.isEmpty()) {
            this.hooks = hooks.toArray(new ConsumerHook[hooks.size()]);
        }
        return this;
    }

    @Override
    public Dispatcher timeoutMillis(long timeoutMillis) {
        if (timeoutMillis > 0) {
            this.timeoutMillis = timeoutMillis;
        }
        return this;
    }

    @Override
    public Dispatcher methodSpecialConfigs(List<MethodSpecialConfig> methodSpecialConfigs) {
        if (!methodSpecialConfigs.isEmpty()) {
            for (MethodSpecialConfig config : methodSpecialConfigs) {
                long timeoutMillis = config.getTimeoutMillis();
                if (timeoutMillis > 0) {
                    methodSpecialTimeoutMapping.put(config.getMethodName(), timeoutMillis);
                }
            }
        }
        return this;
    }

    public long getMethodSpecialTimeoutMillis(String methodName) {
        Long methodTimeoutMillis = methodSpecialTimeoutMapping.get(methodName);
        if (methodTimeoutMillis != null && methodTimeoutMillis > 0) {
            return methodTimeoutMillis;
        }
        return timeoutMillis;
    }

    protected JChannel select(JClient client) {
        // stack copy
        final ServiceMetadata _metadata = metadata;

        CopyOnWriteGroupList groups = client
                .connector()
                .directory(_metadata);
        JChannelGroup group = loadBalancer.select(groups, _metadata);

        if (group != null) {
            if (group.isAvailable()) {
                return group.next();
            }

            // to the deadline (no available channel), the time exceeded the predetermined limit
            long deadline = group.deadlineMillis();
            if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
                boolean removed = groups.remove(group);
                if (removed) {
                    logger.warn("Removed channel group: {} in directory: {} on [select].", group, _metadata.directory());
                }
            }
        } else {
            // for 3 seconds, expired not wait
            if (!client.awaitConnections(_metadata, 3000)) {
                throw new IllegalStateException("no connections");
            }
        }

        JChannelGroup[] snapshot = groups.snapshot();
        for (JChannelGroup g : snapshot) {
            if (g.isAvailable()) {
                return g.next();
            }
        }

        throw new IllegalStateException("no channel");
    }

    // tracing
    protected MessageWrapper doTracing(MessageWrapper message, JChannel channel) {
        if (TracingUtil.isTracingNeeded()) {
            TraceId traceId = TracingUtil.getCurrent();
            if (traceId == TraceId.NULL_TRACE_ID) {
                traceId = TraceId.newInstance(TracingUtil.generateTraceId());
            }
            message.setTraceId(traceId);

            TracingRecorder recorder = TracingUtil.getRecorder();
            recorder.recording(
                    TracingRecorder.Role.CONSUMER, traceId.asText(), metadata.directory(), message.getMethodName(), channel);
        }
        return message;
    }

    protected <T> DefaultInvokeFuture<T> write(
            JChannel channel, final JRequest request, final DefaultInvokeFuture<T> future, final DispatchType dispatchType) {

        final JRequestBytes requestBytes = request.requestBytes();
        final ConsumerHook[] hooks = future.hooks();

        channel.write(requestBytes, new JFutureListener<JChannel>() {

            @SuppressWarnings("all")
            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                // 标记已发送
                future.markSent();

                if (dispatchType == DispatchType.ROUND) {
                    requestBytes.nullBytes();
                }

                // hook.before()
                for (int i = 0; i < hooks.length; i++) {
                    hooks[i].before(request, channel);
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                if (dispatchType == DispatchType.ROUND) {
                    requestBytes.nullBytes();
                }

                if (logger.isWarnEnabled()) {
                    logger.warn("Writes {} fail on {}, {}.", request, channel, stackTrace(cause));
                }

                ResultWrapper result = new ResultWrapper();
                result.setError(new JupiterRemoteException(cause));

                JResponse response = new JResponse(requestBytes.invokeId());
                response.status(Status.CLIENT_ERROR);
                response.result(result);

                DefaultInvokeFuture.received(channel, response);
            }
        });

        return future;
    }
}
