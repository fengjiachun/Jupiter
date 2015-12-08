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

import java.lang.reflect.Field;

import static org.jupiter.common.util.internal.UnsafeAccess.UNSAFE;

/**
 * 基于 {@link ThreadLocal} 的 {@link StringBuilder} 重复利用
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class StringBuilderHelper {

    private static final int DISCARD_LIMIT = 1024 << 3;
    private static final long VALUE_OFFSET;
    static {
        long offset;
        try {
            Field field = Reflects.getField(StringBuilder.class, "value");
            offset = UNSAFE.objectFieldOffset(field);
        } catch (Exception e) {
            offset = 0;
        }
        VALUE_OFFSET = offset;
    }

    private static final ThreadLocal<StringBuilderHelper>
            threadLocalStringBuilderHelper = new ThreadLocal<StringBuilderHelper>() {

        @Override
        protected StringBuilderHelper initialValue() {
            return new StringBuilderHelper();
        }
    };

    public static StringBuilder get() {
        StringBuilderHelper helper = threadLocalStringBuilderHelper.get();
        return helper.getStringBuilder();
    }

    private final StringBuilder buf;

    private StringBuilderHelper() {
        buf = new StringBuilder();
    }

    private StringBuilder getStringBuilder() {
        if (buf.length() > DISCARD_LIMIT) {
            if (VALUE_OFFSET > 0) {
                UNSAFE.putObject(buf, VALUE_OFFSET, new char[32]);
            } else {
                Reflects.setValue(buf, "value", new char[32]);
            }
        }

        buf.setLength(0);
        return buf;
    }
}
