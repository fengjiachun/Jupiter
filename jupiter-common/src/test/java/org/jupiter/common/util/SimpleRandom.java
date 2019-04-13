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

import java.util.concurrent.atomic.AtomicLong;

// Fairly fast random numbers
public final class SimpleRandom {
    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;
    static final AtomicLong seq = new AtomicLong(-715159705);
    private long seed;

    public SimpleRandom() {
        seed = System.nanoTime() + seq.getAndAdd(129);
    }

    public int nextInt() {
        return next();
    }

    public int next() {
        long nextseed = (seed * multiplier + addend) & mask;
        seed = nextseed;
        return ((int) (nextseed >>> 17)) & 0x7FFFFFFF;
    }
}
