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

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class ClassInitializeUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClassInitializeUtil.class);

    /**
     * 提前加载并初始化指定的类, 某些平台下某些类的静态块里面的代码执行实在是太慢了:(
     *
     * @param className         类的全限定名称
     * @param tolerableMillis   超过这个时间打印警告日志
     */
    public static void initClass(String className, long tolerableMillis) {
        long start = System.currentTimeMillis();
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.warn("load class: {} failed.", className);
        }

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > tolerableMillis) {
            logger.warn("{}.<clinit> elapsed: {} millis.", className, elapsed);
        }
    }
}
