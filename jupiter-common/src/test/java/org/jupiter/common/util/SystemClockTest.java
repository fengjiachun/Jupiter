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
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@BenchmarkMode({ Mode.Throughput, Mode.AverageTime, Mode.SampleTime })
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SystemClockTest {

    /*
        Benchmark                 Mode     Cnt     Score       Error   Units
        SystemClockTest.clock1   thrpt       3     0.167 ±     0.212  ops/ns
        SystemClockTest.clock2   thrpt       3    11.499 ±     4.067  ops/ns
        SystemClockTest.clock1    avgt       3    51.302 ±    30.563   ns/op
        SystemClockTest.clock2    avgt       3     0.748 ±     0.232   ns/op
        SystemClockTest.clock1  sample  373913   165.863 ±    46.948   ns/op
        SystemClockTest.clock2  sample  434459    99.446 ±     6.615   ns/op
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SystemClockTest.class.getSimpleName())
                .threads(Runtime.getRuntime().availableProcessors())
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void clock1() {
        System.currentTimeMillis();
    }

    @Benchmark
    public void clock2() {
        SystemClock.millisClock().now();
    }
}
