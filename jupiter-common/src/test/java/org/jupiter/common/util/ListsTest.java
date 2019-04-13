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

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class ListsTest {

    @Test
    public void testTransform() throws Exception {
        List<String> fromList = new LinkedList<>();
        fromList.add("1");
        fromList.add("2");
        fromList.add("3");
        fromList.add("4");

        List<Integer> toList = Lists.transform(fromList, Integer::parseInt);

        assertEquals(toList.size(), fromList.size());
        System.out.println(toList);
    }
}