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

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public abstract class AbstractListenableFuture<V> extends AbstractFuture<V> implements ListenableFuture<V> {

    private Object listeners;

    @Override
    protected void done(int state, Object x) {
        notifyListeners(state, x);
    }

    @Override
    public ListenableFuture<V> addListener(JListener<V> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            addListener0(listener);
        }

        if (isDone()) {
            notifyListeners(state(), outcome());
        }

        return this;
    }

    @Override
    public ListenableFuture<V> addListeners(JListener<V>... listeners) {
        checkNotNull(listeners, "listeners");

        synchronized (this) {
            for (JListener<V> listener : listeners) {
                if (listener == null) {
                    continue;
                }
                addListener0(listener);
            }
        }

        if (isDone()) {
            notifyListeners(state(), outcome());
        }

        return this;
    }

    @Override
    public ListenableFuture<V> removeListener(JListener<V> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            removeListener0(listener);
        }

        return this;
    }

    @Override
    public ListenableFuture<V> removeListeners(JListener<V>... listeners) {
        checkNotNull(listeners, "listeners");

        synchronized (this) {
            for (JListener<V> listener : listeners) {
                if (listener == null) {
                    continue;
                }
                removeListener0(listener);
            }
        }

        return this;
    }

    protected void notifyListeners(int state, Object x) {
        Object listeners;
        synchronized (this) {
            // no competition unless the listener is added too late or the rpc call timeout
            if (this.listeners == null) {
                return;
            }

            listeners = this.listeners;
            this.listeners = null;
        }

        if (listeners instanceof DefaultListeners) {
            JListener<V>[] array = ((DefaultListeners<V>) listeners).listeners();
            int size = ((DefaultListeners<V>) listeners).size();

            for (int i = 0; i < size; i++) {
                notifyListener0(array[i], state, x);
            }
        } else {
            notifyListener0((JListener<V>) listeners, state, x);
        }
    }

    protected abstract void notifyListener0(JListener<V> listener, int state, Object x);

    private void addListener0(JListener<V> listener) {
        if (listeners == null) {
            listeners = listener;
        } else if (listeners instanceof DefaultListeners) {
            ((DefaultListeners<V>) listeners).add(listener);
        } else {
            listeners = DefaultListeners.with((JListener<V>) listeners, listener);
        }
    }

    private void removeListener0(JListener<V> listener) {
        if (listeners instanceof DefaultListeners) {
            ((DefaultListeners<V>) listeners).remove(listener);
        } else if (listeners == listener) {
            listeners = null;
        }
    }
}
