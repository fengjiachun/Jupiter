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

/*
 * Copyright 2013 Ray Tsang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Forked from <a href="https://github.com/jdeferred">JDeferred</a>.
 */
public abstract class AbstractPromise<D, F> implements Promise<D, F> {

    protected volatile State state = State.PENDING;

    protected final List<DoneCallback<D>> doneCallbacks = new CopyOnWriteArrayList<>();
    protected final List<FailCallback<F>> failCallbacks = new CopyOnWriteArrayList<>();

    protected D resolveResult;
    protected F rejectResult;

    @Override
    public State state() {
        return state;
    }

    @Override
    public boolean isPending() {
        return state == State.PENDING;
    }

    @Override
    public boolean isResolved() {
        return state == State.RESOLVED;
    }

    @Override
    public boolean isRejected() {
        return state == State.REJECTED;
    }

    @Override
    public Promise<D, F> then(DoneCallback<D> callback) {
        return done(callback);
    }

    @Override
    public Promise<D, F> then(DoneCallback<D> doneCallback, FailCallback<F> failCallback) {
        done(doneCallback);
        fail(failCallback);
        return this;
    }

    @Override
    public <D_OUT, F_OUT> Promise<D_OUT, F_OUT> then(DonePipe<D, D_OUT, F_OUT> donePipe) {
        return new PipedPromise<>(this, donePipe, null);
    }

    @Override
    public <D_OUT, F_OUT> Promise<D_OUT, F_OUT> then(DonePipe<D, D_OUT, F_OUT> donePipe, FailPipe<F, D_OUT, F_OUT> failPipe) {
        return new PipedPromise<>(this, donePipe, failPipe);
    }

    public Promise<D, F> done(DoneCallback<D> callback) {
        synchronized (this) {
            if (isResolved()) {
                callback.onDone(resolveResult);
            } else {
                doneCallbacks.add(callback);
            }
        }
        return this;
    }

    public Promise<D, F> fail(FailCallback<F> callback) {
        synchronized (this) {
            if (isRejected()) {
                callback.onFail(rejectResult);
            } else {
                failCallbacks.add(callback);
            }
        }
        return this;
    }

    protected void triggerDone(D resolved) {
        for (DoneCallback<D> callback : doneCallbacks) {
            callback.onDone(resolved);
        }
        doneCallbacks.clear();
    }

    protected void triggerFail(F rejected) {
        for (FailCallback<F> callback : failCallbacks) {
            callback.onFail(rejected);
        }
        failCallbacks.clear();
    }
}
