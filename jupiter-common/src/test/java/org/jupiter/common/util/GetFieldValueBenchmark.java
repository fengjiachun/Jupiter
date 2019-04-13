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
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class GetFieldValueBenchmark {

    public static final FieldTest FIELD_TEST = new FieldTest();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(GetFieldValueBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void reflectionGet() {
        Object[] array = (Object[]) Reflects.getValue(FIELD_TEST, "array");
        if ((int) array[0] != 1) {
            System.out.println(1);
        }
    }

    @Benchmark
    public void unsafeGet() {
        // RandomLoadBalancer中有类似代码
        Object[] array = (Object[]) UnsafeUtil.getUnsafe().getObjectVolatile(FIELD_TEST, FieldTest.OFFSET);
        if ((int) array[0] != 1) {
            System.out.println(1);
        }
    }

    @Benchmark
    public void normalGet() {
        if ((int) FIELD_TEST.array[0] != 1) {
            System.out.println(1);
        }
    }
}

class FieldTest {
    public static final long OFFSET;
    static {
        long offsetTmp;
        try {
            offsetTmp = UnsafeUtil.getUnsafe().objectFieldOffset(Reflects.getField(FieldTest.class, "array"));
        } catch (NoSuchFieldException e) {
            offsetTmp = 0;
        }
        OFFSET = offsetTmp;
    }

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    volatile Object[] array = new Object[] { 1, 2 };
}
