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

import java.util.concurrent.atomic.AtomicLong;

/**
 * 带版本号的配置信息, 以保证消息乱序的情况下新数据不被老数据覆盖.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class ConfigWithVersion<T> {

    public static <T> ConfigWithVersion<T> newInstance() {
        return new ConfigWithVersion<>();
    }

    private ConfigWithVersion() {}

    private AtomicLong version = new AtomicLong(0);
    private T config;

    public long getVersion() {
        return version.get();
    }

    public long newVersion() {
        return version.incrementAndGet();
    }

    public T getConfig() {
        return config;
    }

    public void setConfig(T config) {
        this.config = config;
    }
}
