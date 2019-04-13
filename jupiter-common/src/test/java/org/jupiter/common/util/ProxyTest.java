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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

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
 * 这个测试只针对客户端创建代理对象的场景, 不必考虑对Method进行反射调用的开销,
 * 从测试数据上看, byteBuddy性能更优一些, cglib次之, jdkProxy和cglib差距不大,
 * 并且byteBuddy过滤掉Object类方法的代理(toString, hashCode, equals)很方便,
 * jdkProxy和cglib需要在拦截方法里面硬编码单独处理.
 *
 * 另外如果接着需要在拦截方法里进行Method.invoke的话, cglib的FastClass有一些优势.
 *
 * Benchmark                   Mode     Cnt     Score      Error   Units
 * ProxyTest.byteBuddyProxy   thrpt      10     1.100 ±    0.022  ops/ns
 * ProxyTest.cglibProxy       thrpt      10     0.811 ±    0.010  ops/ns
 * ProxyTest.jdkProxy         thrpt      10     0.744 ±    0.026  ops/ns
 * ProxyTest.byteBuddyProxy    avgt      10     0.885 ±    0.020   ns/op
 * ProxyTest.cglibProxy        avgt      10     1.178 ±    0.017   ns/op
 * ProxyTest.jdkProxy          avgt      10     1.302 ±    0.026   ns/op
 * ProxyTest.byteBuddyProxy  sample  124182    43.944 ±    1.977   ns/op
 * ProxyTest.cglibProxy      sample  146889    48.397 ±    1.911   ns/op
 * ProxyTest.jdkProxy        sample  145234    46.235 ±    1.956   ns/op
 * ProxyTest.byteBuddyProxy      ss      10  2400.000 ±  780.720   ns/op
 * ProxyTest.cglibProxy          ss      10  3400.000 ± 1912.365   ns/op
 * ProxyTest.jdkProxy            ss      10  2400.000 ±  780.720   ns/op
 *
 * 2017-01-23 Jupiter中的CGLib依赖已经去掉
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

    static class JdkProxyHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.getName();
        }
    }
    static class ByteBuddyProxyHandler {

        @SuppressWarnings("UnusedParameters")
        @RuntimeType
        public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
            return method.getName();
        }
    }
    static TestInterface jdkProxyObj = Proxies.JDK_PROXY.newProxy(TestInterface.class, new JdkProxyHandler());
    static TestInterface byteBuddyProxyObj = Proxies.BYTE_BUDDY.newProxy(TestInterface.class, new ByteBuddyProxyHandler());

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
    String test1(String arg);
}
