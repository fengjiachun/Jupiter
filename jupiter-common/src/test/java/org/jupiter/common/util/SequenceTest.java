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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SequenceTest {

    static final int THREADS = Runtime.getRuntime().availableProcessors();

    /*
        Benchmark            Mode       Cnt       Score       Error   Units
        SequenceTest.seq1   thrpt       200   36722.867 ±   819.302  ops/ms
        SequenceTest.seq2   thrpt       200  381414.268 ±  6137.577  ops/ms
        SequenceTest.seq1    avgt       200      ≈ 10⁻⁴               ms/op
        SequenceTest.seq2    avgt       200      ≈ 10⁻⁵               ms/op
        SequenceTest.seq1  sample  19925993      ≈ 10⁻³               ms/op
        SequenceTest.seq2  sample  26306839      ≈ 10⁻⁴               ms/op
        SequenceTest.seq1      ss         3    3143.458 ± 12058.710   ns/op
        SequenceTest.seq2      ss         3    2543.208 ±  8317.170   ns/op
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SequenceTest.class.getSimpleName())
                .threads(THREADS)
                .build();

        new Runner(opt).run();
    }

    static AtomicLong seq1 = new AtomicLong();
    static Sequence seq2 = new Sequence(128);

    @Benchmark
    public static long seq1() {
        return seq1.getAndIncrement();
    }

    @Benchmark
    public static long seq2() {
        return seq2.next();
    }
}
