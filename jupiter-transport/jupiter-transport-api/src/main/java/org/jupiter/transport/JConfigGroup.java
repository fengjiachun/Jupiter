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

/**
 * Config group (parent config and child config).
 *
 * 对于网络层的服务端,
 * 通常有一个ServerChannel负责监听并接受连接(它的配置选项对应于 {@link #parent()});
 * 还会有N个负责处理read/write等事件的Channel(它的配置选项对应于 {@link #child()});
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JConfigGroup {

    /**
     * Config for parent.
     */
    JConfig parent();

    /**
     * Config for child.
     */
    JConfig child();
}
