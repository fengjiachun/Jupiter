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

import org.jupiter.rpc.ConsumerHook;
import org.jupiter.rpc.JListener;

/**
 * A {@link InvokeFuture} represents the result of an rpc invocation.
 *
 * This interface provides an abstract view.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("ClassMayBeInterface")
public abstract class InvokeFuture implements JFuture {

    /**
     * Sets hooks for consumer.
     */
    public abstract InvokeFuture hooks(ConsumerHook[] hooks);

    /**
     * Sets listener for asynchronous rpc.
     */
    public abstract InvokeFuture listener(JListener listener);

    /**
     * Sets timestamp on message sent out.
     */
    public abstract void chalkUpSentTimestamp();
}
