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
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.FailOverInvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.exception.JupiterBizException;
import org.jupiter.rpc.exception.JupiterRemoteException;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * 失败自动切换, 当出现失败, 重试其它服务器, 要注意的是重试会带来更长的延时.
 *
 * 建议只用于幂等性操作, 通常比较合适用于读操作.
 *
 * 注意failover不能支持组播的调用方式.
 *
 * https://en.wikipedia.org/wiki/Failover
 *
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailOverClusterInvoker extends AbstractClusterInvoker {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FailOverClusterInvoker.class);

    private final int retries; // 重试次数, 不包含第一次

    public FailOverClusterInvoker(JClient client, Dispatcher dispatcher, int retries) {
        super(client, dispatcher);

        checkArgument(
                dispatcher instanceof DefaultRoundDispatcher,
                Reflects.simpleClassName(dispatcher) + " is unsupported [FailOverClusterInvoker]"
        );

        if (retries >= 0) {
            this.retries = retries;
        } else {
            this.retries = 2;
        }
    }

    @Override
    public String name() {
        return "Fail-over";
    }

    @Override
    public <T> InvokeFuture<T> invoke(String methodName, Object[] args, Class<T> returnType) throws Exception {
        FailOverInvokeFuture<T> future = FailOverInvokeFuture.with(returnType);

        int tryCount = retries + 1;
        invoke0(methodName, args, returnType, tryCount, future, null);

        return future;
    }

    private <T> void invoke0(final String methodName,
                         final Object[] args,
                         final Class<T> returnType,
                         final int tryCount,
                         final FailOverInvokeFuture<T> future,
                         Throwable lastCause) {

        if (tryCount > 0 && isFailoverNeeded(lastCause)) {
            InvokeFuture<T> f = dispatcher.dispatch(client, methodName, args, returnType);

            f.addListener(new JListener<T>() {

                @Override
                public void complete(T result) {
                    future.setSuccess(result);
                }

                @Override
                public void failure(Throwable cause) {
                    logger.warn("[Fail-over] retry, [{}] attempts left, [method: {}], [metadata: {}], {}.",
                            tryCount - 1,
                            methodName,
                            dispatcher.metadata(),
                            stackTrace(cause)
                    );

                    invoke0(methodName, args, returnType, tryCount - 1, future, cause);
                }
            });
        } else {
            future.setFailure(new JupiterRemoteException(name() + " failed: ", lastCause));
        }
    }

    private static boolean isFailoverNeeded(Throwable cause) {
        return cause == null || !(cause instanceof JupiterBizException);
    }
}
