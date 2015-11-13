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

package org.jupiter.hot.exec;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

/**
 * jupiter
 * org.jupiter.hot.exec
 *
 * @author jiachun.fjc
 */
public class HotExecClassLoader extends ClassLoader {

    private static ProtectionDomain PROTECTION_DOMAIN;

    static {
        PROTECTION_DOMAIN = AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {

            @Override
            public ProtectionDomain run() {
                return HotExecClassLoader.class.getProtectionDomain();
            }
        });
    }

    public HotExecClassLoader() {
        super(Thread.currentThread().getContextClassLoader());
    }

    public Class<?> loadBytes(byte[] classBytes) {
        return defineClass(null, classBytes, 0, classBytes.length, PROTECTION_DOMAIN);
    }
}
