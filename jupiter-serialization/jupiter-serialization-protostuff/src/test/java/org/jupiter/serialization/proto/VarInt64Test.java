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

import org.jupiter.common.util.internal.UnsafeDirectBufferUtil;
import org.jupiter.common.util.internal.UnsafeUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

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
        VarInt64Test.writeVarInt64_2  thrpt   10    0.004 ±  0.001  ops/ns
        VarInt64Test.writeVarInt64_3  thrpt   10    0.002 ±  0.001  ops/ns
        VarInt64Test.writeVarInt64_1   avgt   10  161.584 ±  3.377   ns/op
        VarInt64Test.writeVarInt64_2   avgt   10  258.040 ±  7.491   ns/op
        VarInt64Test.writeVarInt64_3   avgt   10  402.007 ± 18.918   ns/op
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
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
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
        if ((value & (0xffffffffffffffffL << 7)) == 0) {
            UnsafeDirectBufferUtil.setByte(address(position), (byte) value);
        } else if ((value & (0xffffffffffffffffL << 14)) == 0) {
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) value & 0x7F) | 0x80) << 8) | (byte) (value >>> 7));
        } else if ((value & (0xffffffffffffffffL << 21)) == 0) {
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) value & 0x7F) | 0x80) << 8) | (((int) value >>> 7 & 0x7F) | 0x80));
            position += 2;
            UnsafeDirectBufferUtil.setByte(address(position), (byte) (value >>> 14));
        } else if ((value & (0xffffffffffffffffL << 28)) == 0) {
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | ((int) (value >>> 21)));
        } else if ((value & (0xffffffffffffffffL << 35)) == 0) {
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80));
            position += 4;
            UnsafeDirectBufferUtil.setByte(address(position), (byte) (value >>> 28));
        } else if ((value & (0xffffffffffffffffL << 42)) == 0) {
            UnsafeDirectBufferUtil.setInt(address(position),
                    ((((int) value & 0x7F) | 0x80) << 24)
                            | ((((int) value >>> 7 & 0x7F) | 0x80) << 16)
                            | ((((int) value >>> 14 & 0x7F) | 0x80) << 8)
                            | (((int) value >>> 21 & 0x7F) | 0x80)
            );
            position += 4;
            UnsafeDirectBufferUtil.setShort(address(position),
                    ((((int) (value >>> 28) & 0x7F) | 0x80) << 8) | (int) (value >>> 35));
        } else if ((value & (0xffffffffffffffffL << 49)) == 0) {
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
        } else if ((value & (0xffffffffffffffffL << 56)) == 0) {
            UnsafeDirectBufferUtil.setLong(address(position),
                    (((value & 0x7F) | 0x80) << 56)
                            | (((value >>> 7 & 0x7F) | 0x80) << 48)
                            | (((value >>> 14 & 0x7F) | 0x80) << 40)
                            | (((value >>> 21 & 0x7F) | 0x80) << 32)
                            | (((value >>> 28 & 0x7F) | 0x80) << 24)
                            | (((value >>> 35 & 0x7F) | 0x80) << 16)
                            | (((value >>> 42 & 0x7F) | 0x80) << 8)
                            | (value >>> 49));
        } else if ((value & (0xffffffffffffffffL << 63)) == 0) {
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
        } else {
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
