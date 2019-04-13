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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 3)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SequenceTest {

    static final int THREADS = Runtime.getRuntime().availableProcessors() << 1;

    /*
        Benchmark           Mode  Cnt    Score     Error   Units
        SequenceTest.seq1  thrpt    3    0.041 ±   0.012  ops/ns
        SequenceTest.seq2  thrpt    3    0.356 ±   0.242  ops/ns
        SequenceTest.seq3  thrpt    3    0.018 ±   0.003  ops/ns
        SequenceTest.seq1   avgt    3  434.871 ± 435.180   ns/op
        SequenceTest.seq2   avgt    3   59.087 ± 109.713   ns/op
        SequenceTest.seq3   avgt    3  854.506 ± 822.850   ns/op
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SequenceTest.class.getSimpleName())
                .threads(THREADS)
                .build();

        new Runner(opt).run();
    }

    static AtomicLong seq1 = new AtomicLong();
    static LongSequence seq2 = new LongSequence(128);
    static long seq3 = 0L;
    static final Object LOCK = new Object();

    @Benchmark
    public static long seq1() {
        return seq1.getAndIncrement();
    }

    @Benchmark
    public static long seq2() {
        return seq2.next();
    }

    @Benchmark
    public static long seq3() {
        synchronized (LOCK) {
            return seq3++;
        }
    }
}
