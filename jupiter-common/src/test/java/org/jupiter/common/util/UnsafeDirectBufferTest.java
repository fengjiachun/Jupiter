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

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.jupiter.common.util.internal.UnsafeDirectBufferUtil;
import org.jupiter.common.util.internal.UnsafeUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * jupiter
 * org.jupiter.serialization.proto
 *
 * @author jiachun.fjc
 */
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class UnsafeDirectBufferTest {

    /*
        Benchmark             Mode  Cnt  Score   Error   Units
        UnsafeTest.setInt_1  thrpt    5  1.862 ± 0.106  ops/ns
        UnsafeTest.setInt_2  thrpt    5  0.722 ± 0.034  ops/ns
        UnsafeTest.setInt_3  thrpt    5  1.851 ± 0.108  ops/ns
        UnsafeTest.setInt_1   avgt    5  0.538 ± 0.040   ns/op
        UnsafeTest.setInt_2   avgt    5  1.397 ± 0.113   ns/op
        UnsafeTest.setInt_3   avgt    5  0.544 ± 0.024   ns/op
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UnsafeDirectBufferTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
    private static final long address = UnsafeUtil.addressOffset(byteBuffer);

    @Benchmark
    public void setInt_1() {
        int value = Integer.MAX_VALUE;
        UnsafeDirectBufferUtil.setInt(address, value);
    }

    @Benchmark
    public void setInt_2() {
        int value = Integer.MAX_VALUE;
        UnsafeDirectBufferUtil.setByte(address, (byte) value);
        UnsafeDirectBufferUtil.setByte(address + 1, (byte) (value >>> 8));
        UnsafeDirectBufferUtil.setByte(address + 2, (byte) (value >>> 16));
        UnsafeDirectBufferUtil.setByte(address + 3, (byte) (value >>> 24));
    }

    @Benchmark
    public void setInt_3() {
        int value = Integer.MAX_VALUE;
        byte b3 = (byte) value;
        byte b2 = (byte) (value >>> 8);
        byte b1 = (byte) (value >>> 16);
        byte b0 = (byte) (value >>> 24);
        UnsafeDirectBufferUtil.setInt(address, makeInt(b3, b2, b1, b0));
    }

    static private int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (((b3) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) << 8) |
                ((b0 & 0xff)));
    }
}
