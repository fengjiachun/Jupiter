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

package org.jupiter.common.promise;

/**
 * jupiter
 * org.jupiter.common.promise
 *
 * @author jiachun.fjc
 */
public interface Promise<D, F> {

    enum State {
        /**
         * The Promise is still pending - it could be created, submitted for execution,
         * or currently running, but not yet finished.
         */
        PENDING,

        /**
         * The Promise has finished running successfully.
         * Thus the Promise is resolved.
         */
        RESOLVED,

        /**
         * The Promise has finished running and a failure occurred.
         * Thus, the Promise is rejected.
         */
        REJECTED
    }

    State state();

    boolean isPending();

    boolean isResolved();

    boolean isRejected();

    Promise<D, F> then(DoneCallback<D> doneCallback);

    Promise<D, F> then(DoneCallback<D> doneCallback, FailCallback<F> failCallback);

    <D_OUT, F_OUT> Promise<D_OUT, F_OUT> then(DonePipe<D, D_OUT, F_OUT> donePipe);

    <D_OUT, F_OUT> Promise<D_OUT, F_OUT> then(DonePipe<D, D_OUT, F_OUT> donePipe, FailPipe<F, D_OUT, F_OUT> failPipe);

    /**
     * This should be called when a task has completed successfully.
     */
    Promise<D, F> resolve(final D resolve);

    /**
     * This should be called when a task has completed unsuccessfully,
     * i.e., a failure may have occurred.
     */
    Promise<D, F> reject(final F reject);

    /**
     * Return an {@link Promise} instance (i.e., an observer). You can register callbacks in this observer.
     */
    Promise<D, F> promise();
}
