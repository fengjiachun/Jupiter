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

package org.jupiter.transport;

import java.util.List;

/**
 * Jupiter transport config.
 *
 * 传输层配置选项, 通常多用于配置网络层参数.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JConfig {

    /**
     * Return all set {@link JOption}'s.
     */
    List<JOption<?>> getOptions();

    /**
     * Return the value of the given {@link JOption}.
     */
    <T> T getOption(JOption<T> option);

    /**
     * Sets a configuration property with the specified name and value.
     *
     * @return {@code true} if and only if the property has been set
     */
    <T> boolean setOption(JOption<T> option, T value);
}
