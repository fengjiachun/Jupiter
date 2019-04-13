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

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

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

    static final Class[] parameterTypes = new Class[] { String.class };
    static final Object[] args = new Object[] { "Jupiter" };
    static class ByteBuddyProxyHandler {

        @SuppressWarnings("unused")
        @RuntimeType
        public Object invoke(@SuperCall Callable<Object> superMethod, @AllArguments @RuntimeType Object[] args) throws Throwable {
            return superMethod.call();
        }
    }
    static ReflectClass1 byteBuddyProxyObj = Proxies.BYTE_BUDDY.newProxy(ReflectClass1.class, new ByteBuddyProxyHandler());
    static ReflectClass1 reflectClass1 = new ReflectClass1();

    static final Method reflectMethod;
    static {
        Method method;
        try {
            method = ReflectClass1.class.getMethod("method", parameterTypes);
        } catch (NoSuchMethodException e) {
            method = null;
        }
        reflectMethod = method;
    }

    @Benchmark
    public void fastInvoke() {
        Reflects.fastInvoke(reflectClass1, "method", parameterTypes, args);
    }

    @Benchmark
    public void jdkReflectInvoke() {
        try {
            reflectMethod.invoke(reflectClass1, "method", args);
        } catch (Throwable ignored) {}
    }

    @Benchmark
    public void commonInvoke() {
        reflectClass1.method("Jupiter");
    }

    @Benchmark
    public void buddyInvoke() {
        byteBuddyProxyObj.method("Jupiter");
    }

    public static class ReflectClass1 {
        public String method(String arg) {
            return arg;
        }
    }
}
