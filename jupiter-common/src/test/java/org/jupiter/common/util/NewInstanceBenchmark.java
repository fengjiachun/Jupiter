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
        jdk7u80:
        ---------------------------------------------------------------------------------------
        Benchmark                                     Mode     Cnt     Score      Error   Units
        NewInstanceBenchmark.jdkNewInstance          thrpt      10     2.875 ±    0.082  ops/ns
        NewInstanceBenchmark.jdkReflectNewInstance   thrpt      10     0.004 ±    0.001  ops/ns
        NewInstanceBenchmark.objenesisNewInstance    thrpt      10     0.062 ±    0.005  ops/ns
        NewInstanceBenchmark.jdkNewInstance           avgt      10     0.357 ±    0.008   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance    avgt      10   229.274 ±    7.063   ns/op
        NewInstanceBenchmark.objenesisNewInstance     avgt      10    15.368 ±    0.207   ns/op
        NewInstanceBenchmark.jdkNewInstance         sample  108700    53.412 ±    2.617   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance  sample  169431   288.834 ±    4.583   ns/op
        NewInstanceBenchmark.objenesisNewInstance   sample  120257    76.868 ±    2.757   ns/op
        NewInstanceBenchmark.jdkNewInstance             ss      10  1000.000 ±  712.696   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance      ss      10  7900.000 ± 4361.446   ns/op
        NewInstanceBenchmark.objenesisNewInstance       ss      10  5600.000 ± 2276.170   ns/op

        jdk8u152:
        ---------------------------------------------------------------------------------------
        Benchmark                                     Mode     Cnt     Score      Error   Units
        NewInstanceBenchmark.jdkNewInstance          thrpt      10     2.875 ±    0.072  ops/ns
        NewInstanceBenchmark.jdkReflectNewInstance   thrpt      10     0.255 ±    0.002  ops/ns
        NewInstanceBenchmark.objenesisNewInstance    thrpt      10     0.129 ±    0.002  ops/ns
        NewInstanceBenchmark.jdkNewInstance           avgt      10     0.347 ±    0.010   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance    avgt      10     3.961 ±    0.044   ns/op
        NewInstanceBenchmark.objenesisNewInstance     avgt      10     7.810 ±    0.108   ns/op
        NewInstanceBenchmark.jdkNewInstance         sample  107021    46.840 ±    1.232   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance  sample  139166    68.646 ±    1.704   ns/op
        NewInstanceBenchmark.objenesisNewInstance   sample   97828    68.328 ±    1.503   ns/op
        NewInstanceBenchmark.jdkNewInstance             ss      10  1003.500 ±  411.726   ns/op
        NewInstanceBenchmark.jdkReflectNewInstance      ss      10  6472.100 ± 1331.522   ns/op
        NewInstanceBenchmark.objenesisNewInstance       ss      10  3882.900 ±  738.171   ns/op
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
        Reflects.newInstance(ForInstanceClass.class, true);
    }

    @Benchmark
    public void jdkNewInstance() {
        new ForInstanceClass();
    }
}

class ForInstanceClass {
}
