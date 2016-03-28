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

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.internal.JUnsafe;

import java.lang.reflect.Constructor;
import java.net.SocketAddress;
import java.util.List;

import static org.jupiter.common.util.Reflects.findMatchingParameterTypes;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryServer extends RegistryMonitor {

    void startRegistryServer();

    @SuppressWarnings("unchecked")
    class Default {

        private static final Class<RegistryServer> defaultRegistryClass;
        private static final List<Class<?>[]> allConstructorsParameterTypes;

        static {
            Class<RegistryServer> cls;
            try {
                cls = (Class<RegistryServer>) Class.forName("org.jupiter.registry.DefaultRegistryServer");
            } catch (ClassNotFoundException e) {
                cls = null;
            }
            defaultRegistryClass = cls;

            if (defaultRegistryClass != null) {
                allConstructorsParameterTypes = Lists.newArrayList();
                Constructor<?>[] array = defaultRegistryClass.getDeclaredConstructors();
                for (Constructor<?> c : array) {
                    allConstructorsParameterTypes.add(c.getParameterTypes());
                }
            } else {
                allConstructorsParameterTypes = null;
            }
        }

        public static RegistryServer createRegistryServer(int port) {
            return newInstance(port);
        }

        public static RegistryServer createRegistryServer(SocketAddress address) {
            return newInstance(address);
        }

        public static RegistryServer createRegistryServer(int port, int nWorks) {
            return newInstance(port, nWorks);
        }

        public static RegistryServer createRegistryServer(SocketAddress address, int nWorks) {
            return newInstance(address, nWorks);
        }

        private static RegistryServer newInstance(Object... parameters) {
            if (defaultRegistryClass == null || allConstructorsParameterTypes == null) {
                throw new UnsupportedOperationException("unsupported default registry");
            }

            Class<?>[] parameterTypes = findMatchingParameterTypes(allConstructorsParameterTypes, parameters);
            if (parameterTypes == null) {
                throw new IllegalArgumentException("parameter types");
            }

            try {
                Constructor<RegistryServer> c = defaultRegistryClass.getConstructor(parameterTypes);
                c.setAccessible(true);
                return c.newInstance(parameters);
            } catch (Exception e) {
                JUnsafe.throwException(e);
            }
            return null; // should never get here
        }
    }
}
