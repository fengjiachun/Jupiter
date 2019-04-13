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
package org.jupiter.rpc.model.metadata;

import java.io.Serializable;

import org.jupiter.rpc.consumer.cluster.ClusterInvoker;

/**
 * Jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ClusterStrategyConfig implements Serializable {

    private static final long serialVersionUID = 8192956131353063709L;

    private ClusterInvoker.Strategy strategy;
    private int failoverRetries;

    public static ClusterStrategyConfig of(String strategy, String failoverRetries) {
        int retries = 0;
        try {
            retries = Integer.parseInt(failoverRetries);
        } catch (Exception ignored) {}

        return of(ClusterInvoker.Strategy.parse(strategy), retries);
    }

    public static ClusterStrategyConfig of(ClusterInvoker.Strategy strategy, int failoverRetries) {
        ClusterStrategyConfig s = new ClusterStrategyConfig();
        s.setStrategy(strategy);
        s.setFailoverRetries(failoverRetries);
        return s;
    }

    public ClusterInvoker.Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ClusterInvoker.Strategy strategy) {
        this.strategy = strategy;
    }

    public int getFailoverRetries() {
        return failoverRetries;
    }

    public void setFailoverRetries(int failoverRetries) {
        this.failoverRetries = failoverRetries;
    }
}
