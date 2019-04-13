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
import java.util.Map;

import org.jupiter.registry.RegisterMeta.ServiceMeta;

/**
 * Registry service.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryService extends Registry {

    /**
     * Register service to registry server.
     */
    void register(RegisterMeta meta);

    /**
     * Unregister service to registry server.
     */
    void unregister(RegisterMeta meta);

    /**
     * Subscribe a service from registry server.
     */
    void subscribe(RegisterMeta.ServiceMeta serviceMeta, NotifyListener listener);

    /**
     * Find a service in the local scope.
     */
    Collection<RegisterMeta> lookup(RegisterMeta.ServiceMeta serviceMeta);

    /**
     * List all consumer's info.
     */
    Map<ServiceMeta, Integer> consumers();

    /**
     * List all provider's info.
     */
    Map<RegisterMeta, RegisterState> providers();

    /**
     * Returns {@code true} if {@link RegistryService} is shutdown.
     */
    boolean isShutdown();

    /**
     * Shutdown.
     */
    void shutdownGracefully();

    enum RegistryType {
        DEFAULT("default"),
        ZOOKEEPER("zookeeper");

        private final String value;

        RegistryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RegistryType parse(String name) {
            for (RegistryType s : values()) {
                if (s.name().equalsIgnoreCase(name)) {
                    return s;
                }
            }
            return null;
        }
    }

    enum RegisterState {
        PREPARE,
        DONE
    }
}
