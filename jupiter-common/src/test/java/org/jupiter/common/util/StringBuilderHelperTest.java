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

import org.junit.Test;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class StringBuilderHelperTest {

    @Test
    public void testGet() throws Exception {
        StringBuilder buf = StringBuilderHelper.get();
        buf.append(new char[1024 * 8 + 1]);
        System.out.println(buf.length());
        System.out.println(((char[]) Reflects.getValue(buf, "value")).length);

        buf = StringBuilderHelper.get();
        System.out.println(buf.length());
        System.out.println(((char[]) Reflects.getValue(buf, "value")).length);
    }
}