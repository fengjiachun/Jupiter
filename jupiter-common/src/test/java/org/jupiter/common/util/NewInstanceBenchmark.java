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
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NewInstanceBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NewInstanceBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void objenesisNewInstance() {
        // ProtoStuffSerializer使用了这样的操作
        Reflects.newInstance(ForInstanceClass.class);
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
