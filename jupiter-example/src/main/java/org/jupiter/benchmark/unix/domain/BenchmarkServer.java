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

package org.jupiter.benchmark.unix.domain;

import io.netty.channel.unix.DomainSocketAddress;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.DefaultServer;
import org.jupiter.rpc.JServer;
import org.jupiter.transport.netty.JNettyDomainAcceptor;

/**
 * 飞行记录: -XX:+UnlockCommercialFeatures -XX:+FlightRecorder
 *
 * jupiter
 * org.jupiter.benchmark.unix.domain
 *
 * @author jiachun.fjc
 */
public class BenchmarkServer {

    public static void main(String[] args) {
//        SystemPropertyUtil.setProperty("jupiter.transport.codec.low_copy", "true");

        final int processors = Runtime.getRuntime().availableProcessors();
        SystemPropertyUtil
                .setProperty("jupiter.executor.factory.provider.core.workers", String.valueOf(processors));
        SystemPropertyUtil
                .setProperty("jupiter.metric.needed", "false");
        SystemPropertyUtil
                .setProperty("jupiter.metric.csv.reporter", "false");
        SystemPropertyUtil
                .setProperty("jupiter.metric.report.period", "1");
        SystemPropertyUtil
                .setProperty("jupiter.executor.factory.provider.queue.capacity", "65536");
        SystemPropertyUtil
                .setProperty("jupiter.executor.factory.affinity.thread", "true");

        // 设置全局provider executor
        SystemPropertyUtil
                .setProperty("jupiter.executor.factory.provider.factory_name", "threadPool");

        final JServer server = new DefaultServer().withAcceptor(new JNettyDomainAcceptor(new DomainSocketAddress(UnixDomainPath.PATH)) {

//            @Override
//            protected ThreadFactory workerThreadFactory(String name) {
//                return new AffinityNettyThreadFactory(name, Thread.MAX_PRIORITY);
//            }
        });
        final MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            server.serviceRegistry()
                    .provider(new ServiceImpl())
                    .register();

            server.acceptor().start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
