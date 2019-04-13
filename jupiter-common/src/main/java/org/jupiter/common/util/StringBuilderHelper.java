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
package org.jupiter.common.util;

import org.jupiter.common.util.internal.InternalThreadLocalMap;

/**
 * 基于 {@link org.jupiter.common.util.internal.InternalThreadLocal} 的 {@link StringBuilder} 重复利用.
 *
 * 注意: 不要在相同的线程中嵌套使用, 太大的StringBuilder也请不要使用这个类, 会导致hold超大块内存一直不释放.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class StringBuilderHelper {

    public static StringBuilder get() {
        return InternalThreadLocalMap.get().stringBuilder();
    }

    private StringBuilderHelper() {}
}
