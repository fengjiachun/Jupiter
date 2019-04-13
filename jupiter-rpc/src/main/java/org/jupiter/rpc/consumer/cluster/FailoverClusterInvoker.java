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
package org.jupiter.rpc.consumer.cluster;

import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.Requires;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.DefaultInvokeFuture;
import org.jupiter.rpc.consumer.future.FailoverInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.transport.channel.JChannel;

/**
 * 失败自动切换, 当出现失败, 重试其它服务器, 要注意的是重试会带来更长的延时.
 *
 * 建议只用于幂等性操作, 通常比较合适用于读操作.
 *
 * 注意failover不能支持广播的调用方式.
 *
 * https://en.wikipedia.org/wiki/Failover
 *
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailoverClusterInvoker implements ClusterInvoker {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FailoverClusterInvoker.class);

    private final Dispatcher dispatcher;
    private final int retries; // 重试次数, 不包含第一次

    public FailoverClusterInvoker(Dispatcher dispatcher, int retries) {
        Requires.requireTrue(
                dispatcher instanceof DefaultRoundDispatcher,
                Reflects.simpleClassName(dispatcher) + " is unsupported [FailoverClusterInvoker]"
        );

        this.dispatcher = dispatcher;
        if (retries >= 0) {
            this.retries = retries;
        } else {
            this.retries = 2;
        }
    }

    @Override
    public Strategy strategy() {
        return Strategy.FAIL_OVER;
    }

    @Override
    public <T> InvokeFuture<T> invoke(JRequest request, Class<T> returnType) throws Exception {
        FailoverInvokeFuture<T> future = FailoverInvokeFuture.with(returnType);

        int tryCount = retries + 1;
        invoke0(request, returnType, tryCount, future, null);

        return future;
    }

    private <T> void invoke0(final JRequest request,
                             final Class<T> returnType,
                             final int tryCount,
                             final FailoverInvokeFuture<T> failOverFuture,
                             final Throwable lastCause) {

        if (tryCount > 0) {
            final InvokeFuture<T> future = dispatcher.dispatch(request, returnType);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    failOverFuture.complete(result);
                } else {
                    if (logger.isWarnEnabled()) {
                        MessageWrapper message = request.message();
                        JChannel channel =
                                future instanceof DefaultInvokeFuture ? ((DefaultInvokeFuture) future).channel() : null;

                        logger.warn("[{}]: [Fail-over] retry, [{}] attempts left, [method: {}], [metadata: {}], {}.",
                                channel,
                                tryCount - 1,
                                message.getMethodName(),
                                message.getMetadata(),
                                StackTraceUtil.stackTrace(throwable));
                    }

                    // Note: Failover uses the same invokeId for each call.
                    //
                    // So if the last call triggered the next call because of a timeout,
                    // and then the previous call returned successfully before the next call returns,
                    // will uses the previous call result
                    invoke0(request, returnType, tryCount - 1, failOverFuture, throwable);
                }
            });
        } else {
            failOverFuture.completeExceptionally(lastCause);
        }
    }
}
