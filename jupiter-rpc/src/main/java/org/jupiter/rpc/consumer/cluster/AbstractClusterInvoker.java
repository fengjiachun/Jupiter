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

package org.jupiter.rpc.consumer.cluster;

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;

/**
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public abstract class AbstractClusterInvoker implements ClusterInvoker {

    protected final JClient client;
    protected final Dispatcher dispatcher;

    public AbstractClusterInvoker(JClient client, Dispatcher dispatcher) {
        this.client = client;
        this.dispatcher = dispatcher;
    }

    @Override
    public String toString() {
        return strategy().name();
    }
}
