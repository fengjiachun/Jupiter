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

package org.jupiter.rpc;

import org.jupiter.registry.Registry;
import org.jupiter.rpc.model.metadata.ServiceWrapper;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JServer extends Registry {

    /**
     * 服务注册表
     */
    interface ServiceRegistry {

        /**
         * 设置服务提供者
         */
        ServiceRegistry provider(Object serviceProvider);

        /**
         * Provider单独的处理器
         */
        ServiceRegistry executor(Executor executor);

        /**
         * 注册服务
         */
        ServiceWrapper register();
    }

    /**
     * 获取一个服务注册表
     */
    ServiceRegistry serviceRegistry();

    /**
     * 查找服务
     */
    ServiceWrapper lookupService(Directory directory);

    /**
     * 移除已注册的服务
     */
    ServiceWrapper removeService(Directory directory);

    /**
     * 获取全部已注册的服务提供者集合
     */
    List<ServiceWrapper> getRegisteredServices();

    /**
     * 发布一个服务
     * @param serviceWrapper 服务提供者, 需要通过{@link ServiceRegistry}构造
     * @param port 服务端口
     */
    void publish(ServiceWrapper serviceWrapper, int port);

    /**
     * 发布一个服务
     * @param serviceWrapper 服务提供者, 需要通过{@link ServiceRegistry}构造
     * @param port 服务端口
     * @param weight 权重
     */
    void publish(ServiceWrapper serviceWrapper, int port, int weight);

    /**
     * 发布一个服务
     * @param serviceWrapper 服务提供者, 需要通过{@link ServiceRegistry}构造
     * @param host 服务地址
     * @param port 服务端口
     * @param weight 权重
     */
    void publish(ServiceWrapper serviceWrapper, String host, int port, int weight);

    /**
     * 发布本地所有服务
     * @param port 服务端口
     */
    void publishAll(int port);

    /**
     * 发布本地所有服务
     * @param port 服务端口
     * @param weight 权重
     */
    void publishAll(int port, int weight);

    /**
     * 发布本地所有服务
     * @param host 服务地址
     * @param port 服务端口
     * @param weight 权重
     */
    void publishAll(String host, int port, int weight);
}
