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

/**
 * 快速失败, 只发起一次调用, 失败立即报错(缺省)
 *
 * 通常用于非幂等性的写操作.
 *
 * https://en.wikipedia.org/wiki/Fail-fast
 *
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public class FailFastClusterInvoker extends ClusterInvoker {

    public FailFastClusterInvoker(JClient client, Dispatcher dispatcher) {
        super(client, dispatcher);
    }

    @Override
    public Object invoke(String methodName, Object[] args, Class<?> returnType) throws Exception {
        Object val = dispatcher.dispatch(client, methodName, args, returnType);
        return ((InvokeFuture<?>) val).getResult();
    }
}
