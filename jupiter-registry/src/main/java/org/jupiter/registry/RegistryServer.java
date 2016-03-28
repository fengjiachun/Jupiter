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

import java.lang.reflect.Constructor;
import java.net.SocketAddress;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryServer extends RegistryMonitor {

    void startRegistryServer();

    @SuppressWarnings({"all"})
    class Default {

        public static RegistryServer newDefault(int port) {
            try {
                return getConstructor(int.class).newInstance(port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static RegistryServer newDefault(SocketAddress address) {
            try {
                return getConstructor(SocketAddress.class).newInstance(address);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static RegistryServer newDefault(int port, int nWorks) {
            try {
                return getConstructor(int.class, int.class).newInstance(port, nWorks);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static RegistryServer newDefault(SocketAddress address, int nWorks) {
            try {
                return getConstructor(SocketAddress.class, int.class).newInstance(address, nWorks);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static Constructor<RegistryServer> getConstructor(Class<?>... classes) {
            try {
                Class<RegistryServer> cls = (Class<RegistryServer>) Class.forName("org.jupiter.registry.DefaultRegistryServer");
                Constructor<RegistryServer> constructor = cls.getConstructor(classes);
                constructor.setAccessible(true);
                return constructor;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
