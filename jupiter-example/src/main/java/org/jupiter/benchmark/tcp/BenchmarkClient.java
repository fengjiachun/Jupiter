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
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 2016-1-9的最新一次测试结果(小数据包1亿+次同步调用):
 * ------------------------------------------------------------------
 * 测试机器:
 * server端(一台机器)
 *      cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
 *      cpu cores: 4
 *
 * client端(一台机器)
 *      cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
 *      cpu cores: 4
 *
 * 网络环境: 局域网
 * ------------------------------------------------------------------
 * 测试结果:
 * 2016-01-09 01:46:38.279 WARN  [main] [BenchmarkClient] - count=128000000
 * 2016-01-09 01:46:38.279 WARN  [main] [BenchmarkClient] - Request count: 128000000, time: 1089 second, qps: 117539
 *
 *
 * 飞行记录: -XX:+UnlockCommercialFeatures -XX:+FlightRecorder
 *
 * jupiter
 * org.jupiter.benchmark.tcp
 *
 * @author jiachun.fjc
 */
public class BenchmarkClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BenchmarkClient.class);

    public static void main(String[] args) {
        int processors = Runtime.getRuntime().availableProcessors();
        SystemPropertyUtil
                .setProperty("jupiter.processor.executor.core.num.workers", String.valueOf(processors));

        NettyConnector connector = new JNettyTcpConnector();
        UnresolvedAddress[] addresses = new UnresolvedAddress[processors];
        for (int i = 0; i < processors; i++) {
            addresses[i] = new UnresolvedAddress("192.168.77.83", 18099);
            connector.connect(addresses[i]);
        }

        final Service service = ProxyFactory
                .factory()
                .connector(connector)
                .addProviderAddress(addresses)
                .interfaceClass(Service.class)
                .newProxyInstance();

        for (int i = 0; i < 10000; i++) {
            try {
                service.hello("jupiter");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final int t = 500000;
        final int step = 6;
        long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(processors << step);
        final AtomicLong count = new AtomicLong();
        for (int i = 0; i < (processors << step); i++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    for (int i = 0; i < t; i++) {
                        try {
                            String result = service.hello("jupiter");

                            if (count.getAndIncrement() % 10000 == 0) {
                                logger.warn("count=" + count.get());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    latch.countDown();
                }
            }).start();
        }
        try {
            latch.await();
            logger.warn("count=" + count.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long second = (System.currentTimeMillis() - start) / 1000;
        logger.warn("Request count: " + count.get() + ", time: " + second + " second, qps: " + count.get() / second);
    }
}
