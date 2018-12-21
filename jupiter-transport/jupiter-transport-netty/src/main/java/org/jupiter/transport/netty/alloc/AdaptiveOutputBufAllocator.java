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

package org.jupiter.transport.netty.alloc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jupiter.common.util.Lists;

import java.util.List;

/**
 * jupiter
 * org.jupiter.transport.alloc
 *
 * @author jiachun.fjc
 */
public class AdaptiveOutputBufAllocator {

    private static final int DEFAULT_MINIMUM = 64;
    private static final int DEFAULT_INITIAL = 512;
    private static final int DEFAULT_MAXIMUM = 524288;

    private static final int INDEX_INCREMENT = 4;
    private static final int INDEX_DECREMENT = 1;

    private static final int[] SIZE_TABLE;

    static {
        List<Integer> sizeTable = Lists.newArrayList();
        for (int i = 16; i < 512; i += 16) {
            sizeTable.add(i);
        }

        for (int i = 512; i > 0; i <<= 1) { // lgtm [java/constant-comparison]
            sizeTable.add(i);
        }

        SIZE_TABLE = new int[sizeTable.size()];
        for (int i = 0; i < SIZE_TABLE.length; i++) {
            SIZE_TABLE[i] = sizeTable.get(i);
        }
    }

    public static final AdaptiveOutputBufAllocator DEFAULT = new AdaptiveOutputBufAllocator();

    private static int getSizeTableIndex(final int size) {
        for (int low = 0, high = SIZE_TABLE.length - 1; ; ) {
            if (high < low) {
                return low;
            }
            if (high == low) {
                return high;
            }

            int mid = low + high >>> 1;
            int a = SIZE_TABLE[mid];
            int b = SIZE_TABLE[mid + 1];
            if (size > b) {
                low = mid + 1;
            } else if (size < a) {
                high = mid - 1;
            } else if (size == a) {
                return mid;
            } else {
                return mid + 1;
            }
        }
    }

    public interface Handle {

        /**
         * Creates a new buffer whose capacity is probably large enough to write all outbound data and small
         * enough not to waste its space.
         */
        ByteBuf allocate(ByteBufAllocator alloc);

        /**
         * Similar to {@link #allocate(ByteBufAllocator)} except that it does not allocate anything but just tells the
         * capacity.
         */
        int guess();

        /**
         * Records the the actual number of wrote bytes in the previous write operation so that the allocator allocates
         * the buffer with potentially more correct capacity.
         *
         * @param actualWroteBytes the actual number of wrote bytes in the previous allocate operation
         */
        void record(int actualWroteBytes);
    }

    private static final class HandleImpl implements Handle {

        private final int minIndex;
        private final int maxIndex;
        private int index;                          // Single IO thread read/write
        private volatile int nextAllocateBufSize;   // Single IO thread read/write, other thread read
        private boolean decreaseNow;                // Single IO thread read/write

        HandleImpl(int minIndex, int maxIndex, int initial) {
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;

            index = getSizeTableIndex(initial);
            nextAllocateBufSize = SIZE_TABLE[index];
        }

        @Override
        public ByteBuf allocate(ByteBufAllocator alloc) {
            return alloc.buffer(guess());
        }

        @Override
        public int guess() {
            return nextAllocateBufSize;
        }

        @Override
        public void record(int actualWroteBytes) {
            if (actualWroteBytes <= SIZE_TABLE[Math.max(0, index - INDEX_DECREMENT - 1)]) {
                if (decreaseNow) {
                    index = Math.max(index - INDEX_DECREMENT, minIndex);
                    nextAllocateBufSize = SIZE_TABLE[index];
                    decreaseNow = false;
                } else {
                    decreaseNow = true;
                }
            } else if (actualWroteBytes >= nextAllocateBufSize) {
                index = Math.min(index + INDEX_INCREMENT, maxIndex);
                nextAllocateBufSize = SIZE_TABLE[index];
                decreaseNow = false;
            }
        }
    }

    private final int minIndex;
    private final int maxIndex;
    private final int initial;

    /**
     * Creates a new predictor with the default parameters.  With the default
     * parameters, the expected buffer size starts from {@code 512}, does not
     * go down below {@code 64}, and does not go up above {@code 524288}.
     */
    private AdaptiveOutputBufAllocator() {
        this(DEFAULT_MINIMUM, DEFAULT_INITIAL, DEFAULT_MAXIMUM);
    }

    /**
     * Creates a new predictor with the specified parameters.
     *
     * @param minimum the inclusive lower bound of the expected buffer size
     * @param initial the initial buffer size when no feed back was received
     * @param maximum the inclusive upper bound of the expected buffer size
     */
    public AdaptiveOutputBufAllocator(int minimum, int initial, int maximum) {
        if (minimum <= 0) {
            throw new IllegalArgumentException("minimum: " + minimum);
        }
        if (initial < minimum) {
            throw new IllegalArgumentException("initial: " + initial);
        }
        if (maximum < initial) {
            throw new IllegalArgumentException("maximum: " + maximum);
        }

        int minIndex = getSizeTableIndex(minimum);
        if (SIZE_TABLE[minIndex] < minimum) {
            this.minIndex = minIndex + 1;
        } else {
            this.minIndex = minIndex;
        }

        int maxIndex = getSizeTableIndex(maximum);
        if (SIZE_TABLE[maxIndex] > maximum) {
            this.maxIndex = maxIndex - 1;
        } else {
            this.maxIndex = maxIndex;
        }

        this.initial = initial;
    }

    public Handle newHandle() {
        return new AdaptiveOutputBufAllocator.HandleImpl(minIndex, maxIndex, initial);
    }
}
