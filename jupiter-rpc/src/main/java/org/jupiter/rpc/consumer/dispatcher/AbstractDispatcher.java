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
import org.jupiter.rpc.DispatchType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.consumer.ConsumerInterceptor;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.exception.JupiterRemoteException;
import org.jupiter.rpc.load.balance.LoadBalancer;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.Status;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JChannelGroup;
import org.jupiter.transport.channel.JFutureListener;
import org.jupiter.transport.payload.JRequestPayload;

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

    private final JClient client;
    private final LoadBalancer loadBalancer;                    // 软负载均衡
    private final Serializer serializerImpl;                    // 序列化/反序列化impl
    private ConsumerInterceptor[] interceptors;                 // 消费者端拦截器
    private long timeoutMillis = JConstants.DEFAULT_TIMEOUT;    // 调用超时时间设置
    // 针对指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodSpecialTimeoutMapping = Maps.newHashMap();

    public AbstractDispatcher(JClient client, SerializerType serializerType) {
        this(client, null, serializerType);
    }

    public AbstractDispatcher(JClient client, LoadBalancer loadBalancer, SerializerType serializerType) {
        this.client = client;
        this.loadBalancer = loadBalancer;
        this.serializerImpl = SerializerFactory.getSerializer(serializerType.value());
    }

    public Serializer serializer() {
        return serializerImpl;
    }

    public ConsumerInterceptor[] interceptors() {
        return interceptors;
    }

    @Override
    public Dispatcher interceptors(List<ConsumerInterceptor> interceptors) {
        if (interceptors != null && !interceptors.isEmpty()) {
            this.interceptors = interceptors.toArray(new ConsumerInterceptor[interceptors.size()]);
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

    protected long getMethodSpecialTimeoutMillis(String methodName) {
        Long methodTimeoutMillis = methodSpecialTimeoutMapping.get(methodName);
        if (methodTimeoutMillis != null && methodTimeoutMillis > 0) {
            return methodTimeoutMillis;
        }
        return timeoutMillis;
    }

    protected JChannel select(ServiceMetadata metadata) {
        CopyOnWriteGroupList groups = client
                .connector()
                .directory(metadata);
        JChannelGroup group = loadBalancer.select(groups, metadata);

        if (group != null) {
            if (group.isAvailable()) {
                return group.next();
            }

            // to the deadline (no available channel), the time exceeded the predetermined limit
            long deadline = group.deadlineMillis();
            if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
                boolean removed = groups.remove(group);
                if (removed) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Removed channel group: {} in directory: {} on [select].",
                                group, metadata.directoryString());
                    }
                }
            }
        } else {
            // for 3 seconds, expired not wait
            if (!client.awaitConnections(metadata, 3000)) {
                throw new IllegalStateException("No connections");
            }
        }

        JChannelGroup[] snapshot = groups.getSnapshot();
        for (JChannelGroup g : snapshot) {
            if (g.isAvailable()) {
                return g.next();
            }
        }

        throw new IllegalStateException("No channel");
    }

    protected JChannelGroup[] groups(ServiceMetadata metadata) {
        return client.connector()
                .directory(metadata)
                .getSnapshot();
    }

    @SuppressWarnings("all")
    protected <T> DefaultInvokeFuture<T> write(
            final JChannel channel, final JRequest request, final Class<T> returnType, final DispatchType dispatchType) {
        final MessageWrapper message = request.message();
        final long timeoutMillis = getMethodSpecialTimeoutMillis(message.getMethodName());
        final ConsumerInterceptor[] interceptors = interceptors();
        final DefaultInvokeFuture<T> future = DefaultInvokeFuture
                .with(request.invokeId(), channel, timeoutMillis, returnType, dispatchType)
                .interceptors(interceptors);

        if (interceptors != null) {
            for (int i = 0; i < interceptors.length; i++) {
                interceptors[i].beforeInvoke(request, channel);
            }
        }

        final JRequestPayload payload = request.payload();

        channel.write(payload, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                // 标记已发送
                future.markSent();

                if (dispatchType == DispatchType.ROUND) {
                    payload.clear();
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                if (dispatchType == DispatchType.ROUND) {
                    payload.clear();
                }

                if (logger.isWarnEnabled()) {
                    logger.warn("Writes {} fail on {}, {}.", request, channel, stackTrace(cause));
                }

                ResultWrapper result = new ResultWrapper();
                result.setError(new JupiterRemoteException(cause));

                JResponse response = new JResponse(payload.invokeId());
                response.status(Status.CLIENT_ERROR);
                response.result(result);

                DefaultInvokeFuture.fakeReceived(channel, response, dispatchType);
            }
        });

        return future;
    }
}
