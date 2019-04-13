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
public class VarInt32Test {

    /*
        Benchmark                      Mode  Cnt    Score    Error   Units
        VarInt32Test.writeVarInt32_1  thrpt   10    0.029 ±  0.001  ops/ns
        VarInt32Test.writeVarInt32_2  thrpt   10    0.039 ±  0.001  ops/ns
        VarInt32Test.writeVarInt32_3  thrpt   10    0.009 ±  0.001  ops/ns
        VarInt32Test.writeVarInt32_1   avgt   10   33.613 ±  0.947   ns/op
        VarInt32Test.writeVarInt32_2   avgt   10   30.271 ±  0.519   ns/op
        VarInt32Test.writeVarInt32_3   avgt   10  117.406 ±  9.018   ns/op
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VarInt32Test.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(5);
    private static final long address = UnsafeUtil.addressOffset(byteBuffer);

    private static final int[] INT_ARRAY_TO_WRITE = new int[] {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            256, 256 * 256, 256 * 256 * 256,
            Integer.MAX_VALUE - 1, Integer.MAX_VALUE
    };

    @Benchmark
    public void writeVarInt32_1() {
        for (int a : INT_ARRAY_TO_WRITE) {
            doWriteVarInt32_1(a);
        }
    }

    @Benchmark
    public void writeVarInt32_2() {
        for (int a : INT_ARRAY_TO_WRITE) {
            doWriteVarInt32_2(a);
        }
    }

    @Benchmark
    public void writeVarInt32_3() {
        for (int a : INT_ARRAY_TO_WRITE) {
            doWriteVarInt32_3(a);
        }
    }

    void doWriteVarInt32_1(int value) {
        int position = byteBuffer.position();
        while (true) {
            if ((value & ~0x7F) == 0) {
                UnsafeDirectBufferUtil.setByte(address(position), (byte) value);
                return;
            } else {
                UnsafeDirectBufferUtil.setByte(address(position++), (byte) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    void doWriteVarInt32_2(int value) {
        int position = byteBuffer.position();
        if ((value & (~0 << 7)) == 0) {
            UnsafeDirectBufferUtil.setByte(address(position), (byte) value);
        } else if ((value & (~0 << 14)) == 0) {
            UnsafeDirectBufferUtil.setShort(address(position),
                    (((value & 0x7F) | 0x80) << 8) | (value >>> 7));
        } else if ((value & (~0 << 21)) == 0) {
            UnsafeDirectBufferUtil.setShort(address(position),
                    (((value & 0x7F) | 0x80) << 8) | ((value >>> 7 & 0x7F) | 0x80));
            UnsafeDirectBufferUtil.setByte(address(position + 2), (byte) (value >>> 14));
        } else if ((value & (~0 << 28)) == 0) {
            UnsafeDirectBufferUtil.setInt(address(position),
                    (((value & 0x7F) | 0x80) << 24)
                            | (((value >>> 7 & 0x7F) | 0x80) << 16)
                            | (((value >>> 14 & 0x7F) | 0x80) << 8)
                            | (value >>> 21));
        } else {
            UnsafeDirectBufferUtil.setInt(address(position),
                    (((value & 0x7F) | 0x80) << 24)
                            | (((value >>> 7 & 0x7F) | 0x80) << 16)
                            | (((value >>> 14 & 0x7F) | 0x80) << 8)
                            | ((value >>> 21 & 0x7F) | 0x80));
            UnsafeDirectBufferUtil.setByte(address(position + 4), (byte) (value >>> 28));
        }
    }

    void doWriteVarInt32_3(int value) {
        byte[] buf = new byte[5];
        int locPtr = 0;
        int position = byteBuffer.position();
        while (true) {
            if ((value & ~0x7F) == 0) {
                buf[locPtr++] = (byte) value;
                UnsafeDirectBufferUtil.setBytes(address(position), buf, 0, locPtr);
                return;
            } else {
                buf[locPtr++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    static long address(int position) {
        return address + position;
    }
}
