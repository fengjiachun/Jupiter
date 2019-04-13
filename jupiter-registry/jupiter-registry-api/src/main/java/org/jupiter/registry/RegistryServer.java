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
import java.util.List;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.ThrowUtil;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryServer extends RegistryMonitor {

    void startRegistryServer();

    /**
     * 用于创建默认的注册中心实现(jupiter-registry-default), 当不使用jupiter-registry-default时, 不能有显示依赖.
     */
    @SuppressWarnings("unchecked")
    class Default {

        private static final Class<RegistryServer> defaultRegistryClass;
        private static final List<Class<?>[]> allConstructorsParameterTypes;

        static {
            Class<RegistryServer> cls;
            try {
                cls = (Class<RegistryServer>) Class.forName(
                        SystemPropertyUtil.get("jupiter.registry.default", "org.jupiter.registry.DefaultRegistryServer"));
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

        public static RegistryServer createRegistryServer(int port, int nWorkers) {
            return newInstance(port, nWorkers);
        }

        public static RegistryServer createRegistryServer(SocketAddress address, int nWorkers) {
            return newInstance(address, nWorkers);
        }

        private static RegistryServer newInstance(Object... parameters) {
            if (defaultRegistryClass == null || allConstructorsParameterTypes == null) {
                throw new UnsupportedOperationException("Unsupported default registry");
            }

            // 根据JLS方法调用的静态分派规则查找最匹配的方法parameterTypes
            Class<?>[] parameterTypes = Reflects.findMatchingParameterTypes(allConstructorsParameterTypes, parameters);
            if (parameterTypes == null) {
                throw new IllegalArgumentException("Parameter types");
            }

            try {
                Constructor<RegistryServer> c = defaultRegistryClass.getConstructor(parameterTypes);
                c.setAccessible(true);
                return c.newInstance(parameters);
            } catch (Exception e) {
                ThrowUtil.throwException(e);
            }
            return null; // should never get here
        }
    }
}
