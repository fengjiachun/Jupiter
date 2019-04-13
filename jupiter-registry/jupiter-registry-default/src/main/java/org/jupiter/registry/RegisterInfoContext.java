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
import java.util.concurrent.ConcurrentMap;

import org.jupiter.common.concurrent.collection.ConcurrentSet;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Requires;

/**
 * 注册服务的全局信息, 同时也供monitor程序使用.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class RegisterInfoContext {

    // 指定服务都有哪些节点注册
    private final ConcurrentMap<RegisterMeta.ServiceMeta, ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>>>
            globalRegisterInfoMap = Maps.newConcurrentMap();
    // 指定节点都注册了哪些服务
    private final ConcurrentMap<RegisterMeta.Address, ConcurrentSet<RegisterMeta.ServiceMeta>>
            globalServiceMetaMap = Maps.newConcurrentMap();

    public ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> getRegisterMeta
            (RegisterMeta.ServiceMeta serviceMeta) {

        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                globalRegisterInfoMap.get(serviceMeta);
        if (config == null) {
            ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> newConfig =
                    ConfigWithVersion.newInstance();
            newConfig.setConfig(Maps.newConcurrentMap());
            config = globalRegisterInfoMap.putIfAbsent(serviceMeta, newConfig);
            if (config == null) {
                config = newConfig;
            }
        }
        return config;
    }

    public ConcurrentSet<RegisterMeta.ServiceMeta> getServiceMeta(RegisterMeta.Address address) {
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = globalServiceMetaMap.get(address);
        if (serviceMetaSet == null) {
            ConcurrentSet<RegisterMeta.ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
            serviceMetaSet = globalServiceMetaMap.putIfAbsent(address, newServiceMetaSet);
            if (serviceMetaSet == null) {
                serviceMetaSet = newServiceMetaSet;
            }
        }
        return serviceMetaSet;
    }

    public Object publishLock(ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config) {
        return Requires.requireNotNull(config, "publish lock");
    }

    // - Monitor -------------------------------------------------------------------------------------------------------

    public List<RegisterMeta.Address> listPublisherHosts() {
        return Lists.newArrayList(globalServiceMetaMap.keySet());
    }

    public List<RegisterMeta.Address> listAddressesByService(RegisterMeta.ServiceMeta serviceMeta) {
        return Lists.newArrayList(getRegisterMeta(serviceMeta).getConfig().keySet());
    }

    public List<RegisterMeta.ServiceMeta> listServicesByAddress(RegisterMeta.Address address) {
        return Lists.newArrayList(getServiceMeta(address));
    }
}
