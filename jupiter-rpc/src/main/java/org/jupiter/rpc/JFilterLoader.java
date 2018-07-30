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

package org.jupiter.rpc;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.List;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JFilterLoader {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JFilterLoader.class);

    public static JFilterChain loadExtFilters(JFilterChain chain, JFilter.Type type) {
        try {
            List<JFilter> sortedList = JServiceLoader.load(JFilter.class).sort();

            // 优先级高的在队首
            for (int i = sortedList.size() - 1; i >= 0; i--) {
                JFilter extFilter = sortedList.get(i);
                JFilter.Type extType = extFilter.getType();
                if (extType == type || extType == JFilter.Type.ALL) {
                    chain = new DefaultFilterChain(extFilter, chain);
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to load extension filters: {}.", stackTrace(t));
        }

        return chain;
    }
}
