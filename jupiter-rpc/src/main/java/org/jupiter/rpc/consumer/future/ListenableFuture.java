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

/**
 * A future that accepts completion listeners.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public interface ListenableFuture<V> {

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * done.  If this future is already completed, the
     * specified listener is notified immediately.
     */
    ListenableFuture<V> addListener(JListener<V> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * done.  If this future is already completed, the
     * specified listeners are notified immediately.
     */
    ListenableFuture<V> addListeners(JListener<V>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is done.  If the specified listener is not associated
     * with this future, this method does nothing and returns silently.
     */
    ListenableFuture<V> removeListener(JListener<V> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is done.  If the specified listeners are not associated
     * with this future, this method does nothing and returns silently.
     */
    ListenableFuture<V> removeListeners(JListener<V>... listeners);
}
