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

package org.jupiter.benchmark.tcp;

import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.transport.JAcceptor;
import org.jupiter.transport.JOption;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * 飞行记录: -XX:+UnlockCommercialFeatures -XX:+FlightRecorder
 *
 * jupiter
 * org.jupiter.benchmark.tcp
 *
 * @author jiachun.fjc
 */
public class BenchmarkServer {

    public static void main(String[] args) {
        final int processors = Runtime.getRuntime().availableProcessors();
        SystemPropertyUtil
                .setProperty("jupiter.processor.executor.core.num.workers", String.valueOf(processors));
        SystemPropertyUtil
                .setProperty("jupiter.metric.csv.reporter", "false");
        SystemPropertyUtil
                .setProperty("jupiter.metric.report.period", "1");
        SystemPropertyUtil
                .setProperty("jupiter.processor.executor.worker.queue.capacity", "65536");

        JAcceptor acceptor = new JNettyTcpAcceptor(18099);
        acceptor.configGroup().child().setOption(JOption.WRITE_BUFFER_HIGH_WATER_MARK, 256 * 1024);
        acceptor.configGroup().child().setOption(JOption.WRITE_BUFFER_LOW_WATER_MARK, 128 * 1024);
        MonitorServer monitor = new MonitorServer();
        try {
            monitor.start();

            acceptor.serviceRegistry()
                    .provider(new ServiceImpl())
                    .register();

            acceptor.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
