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
package org.jupiter.serialization.proto;

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
@Measurement(iterations = 10)
@BenchmarkMode({ Mode.Throughput, Mode.AverageTime })
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class VarInt64Test {

    /*
        Benchmark                      Mode  Cnt    Score    Error   Units
        VarInt64Test.writeVarInt64_1  thrpt   10    0.007 ±  0.001  ops/ns
        VarInt64Test.writeVarInt64_2  thrpt   10    0.007 ±  0.001  ops/ns
        VarInt64Test.writeVarInt64_3  thrpt   10    0.003 ±  0.001  ops/ns
        VarInt64Test.writeVarInt64_1   avgt   10  151.663 ±  4.145   ns/op
        VarInt64Test.writeVarInt64_2   avgt   10  141.639 ±  2.117   ns/op
        VarInt64Test.writeVarInt64_3   avgt   10  345.209 ±  8.962   ns/op
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VarInt64Test.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(10);
    private static final long address = UnsafeUtil.addressOffset(byteBuffer);

    private static final long[] ARRAY_TO_WRITE = new long[] {
            -1, -2, -3, -4, -5, -6, -7, -8, -9, -10,
            -256, -256 * 256, -256 * 256 * 256,
            Long.MIN_VALUE
    };

    @Benchmark
    public void writeVarInt64_1() {
        for (long a : ARRAY_TO_WRITE) {
            doWriteVarInt64_1(a);
        }
    }

    @Benchmark
    public void writeVarInt64_2() {
        for (long a : ARRAY_TO_WRITE) {
            doWriteVarInt64_2(a);
        }
    }

    @Benchmark
    public void writeVarInt64_3() {
        for (long a : ARRAY_TO_WRITE) {
            doWriteVarInt64_3(a);
        }
    }

    void doWriteVarInt64_1(long value) {
        int position = byteBuffer.position();
        while (true) {
            if ((value & ~0x7FL) == 0) {
                UnsafeDirectBufferUtil.setByte(address(position), (byte) value);
                return;
            } else {
                UnsafeDirectBufferUtil.setByte(address(position++), (byte) (((int) value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    void doWriteVarInt64_2(long value) {
        int position = byteBuffer.position();
        // Handle two popular special cases up front ...
        if ((value & (~0L << 7)) == 0) {
            // size == 1
            UnsafeDirectBufferUtil.setByte(address(position), (byte) value);
        } else if (value < 0L) {
            // size == 10
            UnsafeDirectBufferUtil.setLong(address(position),
                    (((value & 0x7F) | 0x80) << 56)
                            | (((value >>> 7 & 0x7F) | 0x80) << 48)
                            | (((value >>> 14 & 0x7F) | 0x80) << 40)
                            | (((value >>> 21 & 0x7F) | 0x80) << 32)
                            | (((value >>> 28 & 0x7F) | 0x80) << 24)
                            | (((value >>> 35 & 0x7F) | 0x80) << 16)
                            | (((value >>> 42 & 0x7F) | 0x80) << 8)
                            | ((value >>> 49 & 0x7F) | 0x80));
            position += 8;
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) (value >>> 56) & 0x7F) | 0x80) << 8) | (int) (value >>> 63));
        }
        // ... leaving us with 8 remaining
        else if ((value & (~0L << 14)) == 0) {
            // size == 2
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) value & 0x7F) | 0x80) << 8) | (byte) (value >>> 7));
        } else if ((value & (~0L << 21)) == 0) {
            // size == 3
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) value & 0x7F) | 0x80) << 8) | (((int) value >>> 7 & 0x7F) | 0x80));
            position += 2;
            UnsafeDirectBufferUtil.setByte(address(position), (byte) (value >>> 14));
        } else if ((value & (~0L << 28)) == 0) {
            // size == 4
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | ((int) (value >>> 21)));
        } else if ((value & (~0L << 35)) == 0) {
            // size == 5
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80));
            position += 4;
            UnsafeDirectBufferUtil.setByte(address(position), (byte) (value >>> 28));
        } else if ((value & (~0L << 42)) == 0) {
            // size == 6
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80)
            );
            position += 4;
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) (value >>> 28) & 0x7F) | 0x80) << 8) | (int) (value >>> 35));
        } else if ((value & (~0L << 49)) == 0) {
            // size == 7
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80)
            );
            position += 4;
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) (value >>> 28) & 0x7F) | 0x80) << 8) | (((int) (value >>> 35) & 0x7F) | 0x80));
            position += 2;
            UnsafeDirectBufferUtil.setByte(address(position), (byte) (value >>> 42));
        } else if ((value & (~0L << 56)) == 0) {
            // size == 8
            UnsafeDirectBufferUtil.setLong(address(position),
                    (((value & 0x7F) | 0x80) << 56)
                            | (((value >>> 7 & 0x7F) | 0x80) << 48)
                            | (((value >>> 14 & 0x7F) | 0x80) << 40)
                            | (((value >>> 21 & 0x7F) | 0x80) << 32)
                            | (((value >>> 28 & 0x7F) | 0x80) << 24)
                            | (((value >>> 35 & 0x7F) | 0x80) << 16)
                            | (((value >>> 42 & 0x7F) | 0x80) << 8)
                            | (value >>> 49));
        } else {
            // size == 9 (value & (~0L << 63)) == 0
            UnsafeDirectBufferUtil.setLong(address(position),
                    (((value & 0x7F) | 0x80) << 56)
                            | (((value >>> 7 & 0x7F) | 0x80) << 48)
                            | (((value >>> 14 & 0x7F) | 0x80) << 40)
                            | (((value >>> 21 & 0x7F) | 0x80) << 32)
                            | (((value >>> 28 & 0x7F) | 0x80) << 24)
                            | (((value >>> 35 & 0x7F) | 0x80) << 16)
                            | (((value >>> 42 & 0x7F) | 0x80) << 8)
                            | ((value >>> 49 & 0x7F) | 0x80));
            position += 8;
            UnsafeDirectBufferUtil.setByte(address(position), (byte) (value >>> 56));
        }
    }

    void doWriteVarInt64_3(long value) {
        byte[] buf = new byte[10];
        int locPtr = 0;
        int position = byteBuffer.position();
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buf[locPtr++] = (byte) value;
                UnsafeDirectBufferUtil.setBytes(address(position), buf, 0, locPtr);
                return;
            } else {
                buf[locPtr++] = (byte) (((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    static long address(int position) {
        return address + position;
    }
}
