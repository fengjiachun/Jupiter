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

import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.future.InvokeFuture;

/**
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
public interface ClusterInvoker {

    /**
     * 集群容错策略
     */
    enum Strategy {
        FAIL_FAST,  // 快速失败
        FAIL_OVER,  // 失败重试
        FAIL_SAFE,  // 失败安全
        // FAIL_BACK,  没想到合适场景, 暂不支持
        // FORKING,    消耗资源太多, 暂不支持
        ;

        public static Strategy parse(String name) {
            for (Strategy s : values()) {
                if (s.name().equalsIgnoreCase(name)) {
                    return s;
                }
            }
            return null;
        }

        public static Strategy getDefault() {
            return FAIL_FAST;
        }
    }

    Strategy strategy();

    <T> InvokeFuture<T> invoke(JRequest request, Class<T> returnType) throws Exception;
}
