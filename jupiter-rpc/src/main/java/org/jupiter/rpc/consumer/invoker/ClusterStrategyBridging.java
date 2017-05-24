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

package org.jupiter.rpc.consumer.invoker;

import org.jupiter.common.util.Maps;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailFastClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailOverClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailSafeClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;

import java.util.List;
import java.util.Map;

/**
 * Jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public abstract class ClusterStrategyBridging {

    private final ClusterInvoker defaultClusterInvoker;
    private final Map<String, ClusterInvoker> methodSpecialClusterInvokerMapping;

    public ClusterStrategyBridging(JClient client,
                                   Dispatcher dispatcher,
                                   ClusterStrategyConfig defaultStrategy,
                                   List<MethodSpecialConfig> methodSpecialConfigs) {

        this.defaultClusterInvoker = createClusterInvoker(client, dispatcher, defaultStrategy);
        this.methodSpecialClusterInvokerMapping = Maps.newHashMap();
        for (MethodSpecialConfig config : methodSpecialConfigs) {
            ClusterStrategyConfig strategy = config.getStrategy();
            if (strategy != null) {
                methodSpecialClusterInvokerMapping.put(
                        config.getMethodName(),
                        createClusterInvoker(client, dispatcher, strategy)
                );
            }
        }
    }

    public ClusterInvoker getClusterInvoker(String methodName) {
        ClusterInvoker invoker = methodSpecialClusterInvokerMapping.get(methodName);
        return invoker != null ? invoker : defaultClusterInvoker;
    }

    private ClusterInvoker createClusterInvoker(JClient client, Dispatcher dispatcher, ClusterStrategyConfig strategy) {
        ClusterInvoker.Strategy s = strategy.getStrategy();
        switch (s) {
            case FAIL_FAST:
                return new FailFastClusterInvoker(client, dispatcher);
            case FAIL_OVER:
                return new FailOverClusterInvoker(client, dispatcher, strategy.getFailoverRetries());
            case FAIL_SAFE:
                return new FailSafeClusterInvoker(client, dispatcher);
            default:
                throw new UnsupportedOperationException("strategy: " + strategy);
        }
    }
}
