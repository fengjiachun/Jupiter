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

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.exception.BizException;
import org.jupiter.rpc.exception.RemoteException;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannel;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 失败自动切换, 当出现失败, 重试其它服务器.
 *
 * 建议只用于幂等性操作, 通常比较合适用于读操作.
 *
 * https://en.wikipedia.org/wiki/Failover
 *
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailOverClusterInvoker extends AbstractClusterInvoker {

    private final int retries; // 重试次数, 不包含第一次

    public FailOverClusterInvoker(JClient client, Dispatcher dispatcher, int retries) {
        super(client, dispatcher);
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
    public Object invoke(String methodName, Object[] args, Class<?> returnType) throws Exception {
        long timeout = dispatcher.getMethodSpecialTimeoutMillis(methodName);
        long start = System.nanoTime();
        Exception cause;
        try {
            Object val = dispatcher.dispatch(client, methodName, args, returnType);
            InvokeFuture<?> future = (InvokeFuture<?>) val; // 组播不支持容错方案, 所以这里一定是InvokeFuture
            return future.getResult();
        } catch (Exception e) {
            if (!failoverNeeded(cause = e)) {
                throw e;
            }

            if ((timeout -= elapsedMillis(start)) <= 0) {
                throw new RemoteException("[Fail-over] timeout: ", e);
            }
        }

        CopyOnWriteGroupList groups = client.connector().directory(dispatcher.getMetadata());
        int tryCount = Math.min(retries, groups.size());
        for (int i = 0; i < tryCount; i++) {
            start = System.nanoTime();
            try {
                JChannel channel = groups.get(i % groups.size()).next();
                Object val = dispatcher.dispatch(client, channel, methodName, args, returnType, timeout);
                InvokeFuture<?> future = (InvokeFuture<?>) val;
                return future.getResult();
            } catch (Exception e) {
                if (failoverNeeded(e)) {
                    if ((timeout -= elapsedMillis(start)) <= 0) {
                        throw new RemoteException("[Fail-over] timeout: ", e);
                    }
                    continue;
                }
                throw e;
            }
        }

        // 全部失败
        throw new RemoteException("[Fail-over] all failed: ", cause);
    }

    private static boolean failoverNeeded(Exception cause) {
        return !(cause instanceof BizException)
                && !(cause instanceof org.jupiter.rpc.exception.TimeoutException)
                && !(cause instanceof java.util.concurrent.TimeoutException);
    }

    private static long elapsedMillis(long start) {
        return NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
