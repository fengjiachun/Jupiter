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
package org.jupiter.rpc.consumer.future;

import java.util.concurrent.CompletableFuture;

/**
 * For broadcast future.
 *
 * 用于支持广播调用的 {@link InvokeFuture}, 不建议也不支持同步获取批量结果.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @see org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher
 *
 * @author jiachun.fjc
 */
public interface InvokeFutureGroup<V> extends InvokeFuture<V> {

    InvokeFuture<V>[] futures();

    CompletableFuture<V>[] toCompletableFutures();
}
