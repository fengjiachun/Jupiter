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

/**
 * Benchmark                   Mode     Cnt     Score      Error   Units
 * ProxyTest.byteBuddyProxy   thrpt      10     1.065 ±    0.035  ops/ns
 * ProxyTest.jdkProxy         thrpt      10     0.712 ±    0.023  ops/ns
 * ProxyTest.byteBuddyProxy    avgt      10     0.932 ±    0.023   ns/op
 * ProxyTest.jdkProxy          avgt      10     1.388 ±    0.028   ns/op
 * ProxyTest.byteBuddyProxy  sample  116019    50.095 ±    2.512   ns/op
 * ProxyTest.jdkProxy        sample  125231    85.375 ±   63.679   ns/op
 * ProxyTest.byteBuddyProxy      ss      10  2300.000 ± 1020.426   ns/op
 * ProxyTest.jdkProxy            ss      10  2200.000 ±  956.183   ns/op
 *
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
    static TestInterface jdkProxyObj = ProxyGenerator.JDK_PROXY.newProxy(TestInterface.class, jdkProxyHandler);
    static TestInterface byteBuddyProxyObj = ProxyGenerator.BYTE_BUDDY.newProxy(TestInterface.class, new ByteBuddyProxyHandler());

    @Benchmark
    public static void jdkProxy() {
        jdkProxyObj.test1("hello");
    }

    @Benchmark
    public static void byteBuddyProxy() {
        byteBuddyProxyObj.test1("hello");
    }
}
interface TestInterface {
    void test1(String arg);
}
