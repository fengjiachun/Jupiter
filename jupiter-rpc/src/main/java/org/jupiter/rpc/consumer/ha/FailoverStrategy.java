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

package org.jupiter.rpc.consumer.ha;

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.exception.BizException;
import org.jupiter.rpc.exception.RemoteException;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.JChannel;

import java.lang.reflect.Method;

/**
 * 失败重试, 业务需保证自己的服务是幂等的
 *
 * 注意:
 * 对于Failover的容错方案, 我本人并不认为是常规性配置, 为了简化使用同学的配置, 所以该策略是服务级别的配置,
 * 对于非幂等的方法使用该策略时请务必小心这个大坑.
 *
 * https://en.wikipedia.org/wiki/Failover
 *
 * jupiter
 * org.jupiter.rpc.consumer.ha
 *
 * @author jiachun.fjc
 */
public class FailoverStrategy extends AbstractHaStrategy {

    private final int retries;

    public FailoverStrategy(JClient client, Dispatcher dispatcher, int retries) {
        super(client, dispatcher);
        if (retries >= 0) {
            this.retries = retries;
        } else {
            this.retries = 3;
        }
    }

    @Override
    public Object invoke(Method method, Object[] args) throws Exception {
        Exception cause;
        try {
            Object val = dispatcher.dispatch(client, method.getName(), args, method.getReturnType());
            InvokeFuture<?> future = (InvokeFuture<?>) val; // 组播不支持容错方案, 所以这里一定是InvokeFuture
            return future.getResult();
        } catch (Exception e) {
            if (!failoverNeeded(cause = e)) {
                throw e;
            }
        }

        CopyOnWriteGroupList groups = client.connector().directory(dispatcher.getMetadata());
        for (int i = 0; i < retries; i++) {
            try {
                JChannel channel = groups.get(i % groups.size()).next();
                Object val = dispatcher.dispatch(client, channel, method.getName(), args, method.getReturnType());
                InvokeFuture<?> future = (InvokeFuture<?>) val;
                return future.getResult();
            } catch (Exception e) {
                if (failoverNeeded(e)) {
                    continue;
                }
                throw e;
            }
        }

        // 全部失败
        throw new RemoteException("[failover] all failed: ", cause);
    }

    public boolean failoverNeeded(Exception cause) {
        return !(cause instanceof BizException)
                && !(cause instanceof org.jupiter.rpc.exception.TimeoutException)
                && !(cause instanceof java.util.concurrent.TimeoutException);
    }
}
