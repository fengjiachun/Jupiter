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

package org.jupiter.registry;

import java.util.Collection;

import static org.jupiter.registry.RegisterMeta.*;

/**
 * Registry service.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryService {

    /**
     * 初始化并与ConfigServer建立连接
     */
    void init(Object... args);

    /**
     * 注册服务
     */
    void register(RegisterMeta meta);

    /**
     * 取消注册服务
     */
    void unregister(RegisterMeta meta);

    /**
     * 订阅服务
     */
    void subscribe(ServiceMeta serviceMeta, NotifyListener listener);

    /**
     * provider下线通知
     */
    void offlineListening(Address address, OfflineListener listener);

    /**
     * 本地查找服务
     */
    Collection<RegisterMeta> lookup(ServiceMeta serviceMeta);
}
