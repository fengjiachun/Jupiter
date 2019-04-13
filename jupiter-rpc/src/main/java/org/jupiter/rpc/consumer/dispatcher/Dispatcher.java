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

import java.util.List;

import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.ConsumerInterceptor;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public interface Dispatcher {

    <T> InvokeFuture<T> dispatch(JRequest request, Class<T> returnType);

    Dispatcher interceptors(List<ConsumerInterceptor> interceptors);

    Dispatcher timeoutMillis(long timeoutMillis);

    Dispatcher methodSpecialConfigs(List<MethodSpecialConfig> methodSpecialConfigs);
}
