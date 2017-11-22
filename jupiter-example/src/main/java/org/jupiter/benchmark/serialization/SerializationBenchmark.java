package org.jupiter.benchmark.serialization;

import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.SerializerType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SerializationBenchmark {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SerializationBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static final Serializer JAVA_SERIALIZER = SerializerFactory.getSerializer(SerializerType.JAVA.value());
    private static final Serializer KRYO_SERIALIZER = SerializerFactory.getSerializer(SerializerType.KRYO.value());

    @Benchmark
    public void javaSerialization() {
        Base base = new Base();
        byte[] bytes = JAVA_SERIALIZER.writeObject(base);
        Base base1 = JAVA_SERIALIZER.readObject(bytes, Base.class);
    }


    @Benchmark
    public void kryoSerialization() {
        Base base = new Base();
        byte[] bytes = KRYO_SERIALIZER.writeObject(base);
        Base base1 = KRYO_SERIALIZER.readObject(bytes, Base.class);
    }

    static class Base implements Serializable {
        List<Integer> list = new ArrayList<>();
        Base() {
            for (int i = 0; i < 100; i++) {
                list.add(i);
            }
        }
    }
}
