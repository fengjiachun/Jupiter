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

import org.jupiter.rpc.JListener;
import org.jupiter.rpc.aop.ConsumerHook;

import java.util.List;

/**
 * Invoke future.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
public interface InvokeFuture {

    /**
     * Sets hooks for consumer
     */
    InvokeFuture hooks(List<ConsumerHook> hooks);

    /**
     * Sets listener for asynchronous rpc
     */
    InvokeFuture listener(JListener listener);

    /**
     * Sets send time
     */
    void sent();

    /**
     * Returns the result for rpc
     */
    Object singleResult() throws Throwable;
}
