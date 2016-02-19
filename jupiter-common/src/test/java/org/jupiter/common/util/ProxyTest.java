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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

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
public class ProxyTest {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ProxyTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    static InvocationHandler jdkProxyHandler = new InvocationHandler() {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.getName();
        }
    };
    static class ByteBuddyProxyHandler {

        @SuppressWarnings("UnusedParameters")
        @RuntimeType
        public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
            return method.getName();
        }
    }
    static TestInterface jdkProxyObj = Reflects.newProxy(TestInterface.class, jdkProxyHandler);
    static TestInterface byteBuddyProxyObj;
    static {
        try {
            byteBuddyProxyObj = new ByteBuddy()
                    .subclass(TestInterface.class)
                    .method(isDeclaredBy(TestInterface.class))
                    .intercept(MethodDelegation.to(new ByteBuddyProxyHandler(), "handler")
//                            .filter(not(isDeclaredBy(Object.class)))
                    )
                    .make()
                    .load(TestInterface.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded()
                    .newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Benchmark
    public static void jdkProxy() {
        jdkProxyObj.test1();
    }

    @Benchmark
    public static void byteBuddyProxy() {
        byteBuddyProxyObj.test1();
    }
}
interface TestInterface {
    void test1();
}
