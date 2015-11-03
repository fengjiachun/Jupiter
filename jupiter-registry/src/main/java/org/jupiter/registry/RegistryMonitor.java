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

import java.util.List;

/**
 * Registry monitor.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryMonitor {

    /**
     * 获取所有Publisher的地址列表
     */
    List<String> listPublisherHosts();

    /**
     * 获取所有Subscriber的地址列表
     */
    List<String> listSubscriberAddresses();

    /**
     * 根据指定服务返回改服务的全部提供者的地址
     */
    List<String> listAddressesByService(String group, String version, String serviceProviderName);

    /**
     * 根据指定地址返回该地址对应的节点提供的所有服务
     */
    List<String> listServicesByAddress(String host, int port);
}
