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

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NewInstanceBenchmark {

    /**
        jdk7u80结果:
        ---------------------------------------------------------------------------------------
        Benchmark                                     Mode     Cnt     Score      Error   Units
        NewInstanceBenchmark.jdkNewInstance          thrpt      10     2.912 ±    0.051  ops/ns
        NewInstanceBenchmark.jdkReflectNewInstance   thrpt      10     0.004 ±    0.001  ops/ns
        NewInstanceBenchmark.objenesisNewInstance    thrpt      10     0.067 ±    0.001  ops/ns
        NewInstanceBenchmark.jdkNewInstance           avgt      10     0.342 ±    0.004   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance    avgt      10   221.239 ±    2.308   ns/op
        NewInstanceBenchmark.objenesisNewInstance     avgt      10    15.077 ±    0.332   ns/op
        NewInstanceBenchmark.jdkNewInstance         sample  110386    54.979 ±    2.569   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance  sample  174726   286.592 ±    5.322   ns/op
        NewInstanceBenchmark.objenesisNewInstance   sample  124701    76.654 ±    3.128   ns/op
        NewInstanceBenchmark.jdkNewInstance             ss      10   600.000 ±  780.720   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance      ss      10  5600.000 ±  780.720   ns/op
        NewInstanceBenchmark.objenesisNewInstance       ss      10  6300.000 ± 4279.147   ns/op

        jdk8u152结果:
        ---------------------------------------------------------------------------------------
        Benchmark                                     Mode     Cnt     Score      Error   Units
        NewInstanceBenchmark.jdkNewInstance          thrpt      10     2.860 ±    0.086  ops/ns
        NewInstanceBenchmark.jdkReflectNewInstance   thrpt      10     0.257 ±    0.004  ops/ns
        NewInstanceBenchmark.objenesisNewInstance    thrpt      10     0.126 ±    0.002  ops/ns
        NewInstanceBenchmark.jdkNewInstance           avgt      10     0.346 ±    0.004   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance    avgt      10     3.894 ±    0.036   ns/op
        NewInstanceBenchmark.objenesisNewInstance     avgt      10     7.748 ±    0.109   ns/op
        NewInstanceBenchmark.jdkNewInstance         sample  109530    44.991 ±    1.818   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance  sample  142769    66.277 ±    1.204   ns/op
        NewInstanceBenchmark.objenesisNewInstance   sample   98286    69.301 ±    2.145   ns/op
        NewInstanceBenchmark.jdkNewInstance             ss      10   780.900 ±  208.992   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance      ss      10  8125.200 ± 6525.499   ns/op
        NewInstanceBenchmark.objenesisNewInstance       ss      10  3635.500 ± 1165.929   ns/op
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NewInstanceBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void objenesisNewInstance() {
        // ProtoStuffSerializer中有类似代码
        Reflects.newInstance(ForInstanceClass.class, false);
    }

    @Benchmark
    public void jdkReflectNewInstance() {
        try {
            ForInstanceClass.class.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Benchmark
    public void jdkNewInstance() {
        new ForInstanceClass();
    }
}

class ForInstanceClass {
}
