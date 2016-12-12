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

import java.lang.reflect.Method;

/**
 * 快速失败, 默认容错方案
 *
 * https://en.wikipedia.org/wiki/Fail-fast
 *
 * jupiter
 * org.jupiter.rpc.consumer.ha
 *
 * @author jiachun.fjc
 */
public class FailFastStrategy_changeName extends AbstractHaStrategy {

    public FailFastStrategy_changeName(JClient client, Dispatcher dispatcher) {
        super(client, dispatcher);
    }

    @Override
    public Object invoke(Method method, Object[] args) throws Exception {
        Object val = dispatcher.dispatch(client, method.getName(), args, method.getReturnType());
        return ((InvokeFuture<?>) val).getResult();
    }
}
