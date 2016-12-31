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

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * 失败安全, 出现异常时, 直接忽略.
 *
 * 通常用于写入审计日志等操作.
 *
 * http://en.wikipedia.org/wiki/Fail-safe
 *
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailSafeClusterInvoker extends ClusterInvoker {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FailSafeClusterInvoker.class);

    public FailSafeClusterInvoker(JClient client, Dispatcher dispatcher) {
        super(client, dispatcher);
    }

    @Override
    public Object invoke(String methodName, Object[] args, Class<?> returnType) throws Exception {
        try {
            Object val = dispatcher.dispatch(client, methodName, args, returnType);
            return ((InvokeFuture<?>) val).getResult();
        } catch (Throwable t) {
            logger.warn("Ignored exception on [Fail-safe] cluster invoker: {}.", stackTrace(t));
        }
        return null;
    }
}
