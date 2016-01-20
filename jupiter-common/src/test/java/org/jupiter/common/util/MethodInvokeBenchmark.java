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
public class MethodInvokeBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MethodInvokeBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    static final Class[] params = new Class[] { String.class };

    @Benchmark
    public void cglibFastInvoke() {
        ReflectClass1 obj = new ReflectClass1();
        Reflects.fastInvoke(obj, "method", params, new Object[] { "Jupiter" });
    }

    @Benchmark
    public void jdkReflectInvoke() {
        ReflectClass1 obj = new ReflectClass1();
        Reflects.invoke(obj, "method", params, new Object[] { "Jupiter" });
    }

    @Benchmark
    public void commonInvoke() {
        ReflectClass1 obj = new ReflectClass1();
        obj.method("Jupiter");
    }
}

class ReflectClass1 {
    public String method(String arg) {
        return "Hello " + arg;
    }
}
