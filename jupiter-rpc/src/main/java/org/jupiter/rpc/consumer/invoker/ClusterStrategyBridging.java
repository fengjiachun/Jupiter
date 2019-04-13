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

import java.util.List;
import java.util.Map;

import org.jupiter.common.util.Maps;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailfastClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailoverClusterInvoker;
import org.jupiter.rpc.consumer.cluster.FailsafeClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;

/**
 * Jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public class ClusterStrategyBridging {

    private final ClusterInvoker defaultClusterInvoker;
    private final Map<String, ClusterInvoker> methodSpecialClusterInvokerMapping;

    public ClusterStrategyBridging(Dispatcher dispatcher,
                                   ClusterStrategyConfig defaultStrategy,
                                   List<MethodSpecialConfig> methodSpecialConfigs) {
        this.defaultClusterInvoker = createClusterInvoker(dispatcher, defaultStrategy);
        this.methodSpecialClusterInvokerMapping = Maps.newHashMap();

        for (MethodSpecialConfig config : methodSpecialConfigs) {
            ClusterStrategyConfig strategy = config.getStrategy();
            if (strategy != null) {
                methodSpecialClusterInvokerMapping.put(
                        config.getMethodName(),
                        createClusterInvoker(dispatcher, strategy)
                );
            }
        }
    }

    public ClusterInvoker findClusterInvoker(String methodName) {
        ClusterInvoker invoker = methodSpecialClusterInvokerMapping.get(methodName);
        return invoker != null ? invoker : defaultClusterInvoker;
    }

    private ClusterInvoker createClusterInvoker(Dispatcher dispatcher, ClusterStrategyConfig strategy) {
        ClusterInvoker.Strategy s = strategy.getStrategy();
        switch (s) {
            case FAIL_FAST:
                return new FailfastClusterInvoker(dispatcher);
            case FAIL_OVER:
                return new FailoverClusterInvoker(dispatcher, strategy.getFailoverRetries());
            case FAIL_SAFE:
                return new FailsafeClusterInvoker(dispatcher);
            default:
                throw new UnsupportedOperationException("Unsupported strategy: " + strategy);
        }
    }
}
