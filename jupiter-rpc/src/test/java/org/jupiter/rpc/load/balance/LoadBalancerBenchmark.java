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
package org.jupiter.rpc.load.balance;

import java.util.concurrent.TimeUnit;

import org.jupiter.transport.Directory;
import org.jupiter.transport.channel.CopyOnWriteGroupList;
import org.jupiter.transport.channel.DirectoryJChannelGroup;
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
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LoadBalancerBenchmark {

    /*
        Benchmark                                              Mode     Cnt       Score      Error   Units
        LoadBalancerBenchmark.random                          thrpt      10       0.016 ±    0.001  ops/ns
        LoadBalancerBenchmark.roundRobin                      thrpt      10       0.005 ±    0.001  ops/ns
        LoadBalancerBenchmark.random                           avgt      10      66.000 ±    4.801   ns/op
        LoadBalancerBenchmark.roundRobin                       avgt      10     198.770 ±    6.037   ns/op
        LoadBalancerBenchmark.random                         sample  286448     137.343 ±    3.470   ns/op
        LoadBalancerBenchmark.random:random·p0.00            sample              38.000              ns/op
        LoadBalancerBenchmark.random:random·p0.50            sample             115.000              ns/op
        LoadBalancerBenchmark.random:random·p0.90            sample             138.000              ns/op
        LoadBalancerBenchmark.random:random·p0.95            sample             147.000              ns/op
        LoadBalancerBenchmark.random:random·p0.99            sample             186.000              ns/op
        LoadBalancerBenchmark.random:random·p0.999           sample            9552.000              ns/op
        LoadBalancerBenchmark.random:random·p0.9999          sample           20668.995              ns/op
        LoadBalancerBenchmark.random:random·p1.00            sample          108160.000              ns/op
        LoadBalancerBenchmark.roundRobin                     sample  319596     259.593 ±    8.938   ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p0.00    sample             120.000              ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p0.50    sample             213.000              ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p0.90    sample             272.000              ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p0.95    sample             356.000              ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p0.99    sample             595.000              ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p0.999   sample           10592.000              ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p0.9999  sample           22947.869              ns/op
        LoadBalancerBenchmark.roundRobin:roundRobin·p1.00    sample          781312.000              ns/op
        LoadBalancerBenchmark.random                             ss      10   10290.400 ± 5508.110   ns/op
        LoadBalancerBenchmark.roundRobin                         ss      10   17424.000 ± 7863.309   ns/op
     */

    static final CopyOnWriteGroupList groupList = new CopyOnWriteGroupList(new DirectoryJChannelGroup());
    static final Directory directory = new Directory() {
        @Override
        public String getGroup() {
            return "test";
        }

        @Override
        public String getServiceProviderName() {
            return "test";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }
    };

    static {
        int len = 100;
        for (int i = 0; i < len; i++) {
            ChannelGroup c = new ChannelGroup();
            c.index = i;
            c.weight = (i == 5 ? 10 : 2);
            groupList.addIfAbsent(c);
        }
    }

    static final LoadBalancer rr = new RoundRobinLoadBalancer();
    static final LoadBalancer rm = new RandomLoadBalancer();

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LoadBalancerBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void roundRobin() {
        rr.select(groupList, directory);
    }

    @Benchmark
    public void random() {
        rm.select(groupList, directory);
    }
}
