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

    @Benchmark
    public void cglibFastInvoke() {
        // RecyclableMessageTask使用了这样的操作
        ReflectClass1 obj = new ReflectClass1();
        Reflects.fastInvoke(obj, "method", new Class[] { String.class }, new Object[] { "Jupiter" });
    }

    @Benchmark
    public void jdkReflectInvoke() {
        ReflectClass1 obj = new ReflectClass1();
        Reflects.invoke(obj, "method", new Class[] { String.class }, new Object[] { "Jupiter" });
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
