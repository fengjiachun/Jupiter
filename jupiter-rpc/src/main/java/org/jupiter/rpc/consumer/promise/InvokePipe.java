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

package org.jupiter.rpc.consumer.promise;

import org.jupiter.common.concurrent.promise.DonePipe;
import org.jupiter.rpc.consumer.invoker.PromiseInvoker;

/**
 * jupiter
 * org.jupiter.rpc.consumer.promise
 *
 * @author jiachun.fjc
 */
public abstract class InvokePipe implements DonePipe<Object, Object, Throwable> {

    @Override
    public JPromise pipeDone(Object result) {
        doInPipe(result);
        return PromiseInvoker.promise();
    }

    public abstract void doInPipe(Object result);
}
