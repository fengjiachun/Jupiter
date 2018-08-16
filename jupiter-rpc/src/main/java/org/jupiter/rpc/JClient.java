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

import org.jupiter.registry.*;
import org.jupiter.transport.Directory;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.UnresolvedAddress;

import java.util.Collection;

/**
 * The jupiter rpc client.
 *
 * 注意 JClient 单例即可, 不要创建多个实例.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JClient extends Registry {

    /**
     * 每一个应用都建议设置一个appName.
     */
    String appName();

    /**
     * 网络层connector.
     */
    JConnector<JConnection> connector();

    /**
     * 设置网络层connector.
     */
    JClient withConnector(JConnector<JConnection> connector);

    /**
     * 注册服务实例
     */
    RegistryService registryService();

    /**
     * 查找服务信息.
     */
    Collection<RegisterMeta> lookup(Directory directory);

    /**
     * 设置对指定服务由jupiter自动管理连接.
     */
    JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass);

    /**
     * 设置对指定服务由jupiter自动管理连接.
     */
    JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass, String version);

    /**
     * 设置对指定服务由jupiter自动管理连接.
     */
    JConnector.ConnectionWatcher watchConnections(Directory directory);

    /**
     * 阻塞等待一直到该服务有可用连接或者超时.
     */
    boolean awaitConnections(Class<?> interfaceClass, long timeoutMillis);

    /**
     * 阻塞等待一直到该服务有可用连接或者超时.
     */
    boolean awaitConnections(Class<?> interfaceClass, String version, long timeoutMillis);

    /**
     * 阻塞等待一直到该服务有可用连接或者超时.
     */
    boolean awaitConnections(Directory directory, long timeoutMillis);

    /**
     * 从注册中心订阅一个服务.
     */
    void subscribe(Directory directory, NotifyListener listener);

    /**
     * 服务下线通知.
     */
    void offlineListening(UnresolvedAddress address, OfflineListener listener);

    /**
     * 优雅关闭jupiter client.
     */
    void shutdownGracefully();
}
