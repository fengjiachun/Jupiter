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

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JListener;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * 用于实现failover集群容错方案的 {@link InvokeFuture}.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @see org.jupiter.rpc.consumer.cluster.FailSafeClusterInvoker
 *
 * @author jiachun.fjc
 */
public class FailOverInvokeFuture<V> extends AbstractListenableFuture<V> implements InvokeFuture<V> {
    // 不要在意FailOver的'O'为什么是大写, 因为要和FailFast, FailSafe等单词看着风格一样我心里才舒服

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FailOverInvokeFuture.class);

    private final Class<V> returnType;

    public static <T> FailOverInvokeFuture<T> with(Class<T> returnType) {
        return new FailOverInvokeFuture<>(returnType);
    }

    private FailOverInvokeFuture(Class<V> returnType) {
        this.returnType = returnType;
    }

    public void setSuccess(V result) {
        set(result);
    }

    public void setFailure(Throwable cause) {
        setException(cause);
    }

    @Override
    public Class<V> returnType() {
        return returnType;
    }

    @Override
    public V getResult() throws Throwable {
        return get();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void notifyListener0(JListener<V> listener, int state, Object x) {
        try {
            if (state == NORMAL) {
                listener.complete((V) x);
            } else {
                listener.failure((Throwable) x);
            }
        } catch (Throwable t) {
            logger.error("An exception was thrown by {}.{}, {}.",
                    listener.getClass().getName(), state == NORMAL ? "complete()" : "failure()", stackTrace(t));
        }
    }
}
