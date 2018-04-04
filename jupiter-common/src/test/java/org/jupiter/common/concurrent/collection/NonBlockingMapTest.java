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

package org.jupiter.common.concurrent.collection;

/**
 * for debug
 *
 * jupiter
 * org.jupiter.common.concurrent.collection
 *
 * @author jiachun.fjc
 */
public class NonBlockingMapTest {

    public static void main(String[] args) {
        NonBlockingHashMap<Integer, String> map = new NonBlockingHashMap<>();
        for (int i = 0; i < 100; i++) {
            map.put(i, "--" + i);
        }
        String val = map.get(3);
        System.out.println(val);
    }
}
