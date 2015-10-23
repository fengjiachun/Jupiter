package org.jupiter.common.util;

import org.jupiter.common.util.internal.UnsafeAccess;
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
        if (array[0] != 1) {
            System.out.println(1);
        }
    }

    @Benchmark
    public void unsafeGet() {
        // RandomLoadBalance中使用了这样的操作
        Object[] array = (Object[]) UnsafeAccess.UNSAFE.getObjectVolatile(FIELD_TEST, FieldTest.OFFSET);
        if (array[0] != 1) {
            System.out.println(1);
        }
    }

    @Benchmark
    public void normalGet() {
        if (FIELD_TEST.array[0] != 1) {
            System.out.println(1);
        }
    }
}

class FieldTest {
    public static final long OFFSET;
    static {
        long offsetTmp;
        try {
            offsetTmp = UnsafeAccess.UNSAFE.objectFieldOffset(Reflects.getField(FieldTest.class, "array"));
        } catch (NoSuchFieldException e) {
            offsetTmp = 0;
        }
        OFFSET = offsetTmp;
    }

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    volatile Object[] array = new Object[] { 1, 2 };
}
