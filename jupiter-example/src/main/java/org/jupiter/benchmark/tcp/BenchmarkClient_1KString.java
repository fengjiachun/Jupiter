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

import org.jupiter.common.util.SystemClock;
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
 * jupiter
 * org.jupiter.benchmark.tcp
 *
 * @author jiachun.fjc
 */
public class BenchmarkClient_1KString {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BenchmarkClient_1KString.class);

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

        StringBuilder buf = new StringBuilder(1024);
        for (int i = 0; i < 1000; i++) {
            buf.append(0);
        }
        final String data = buf.toString();

        for (int i = 0; i < 10000; i++) {
            try {
                service.hello(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final int t = 50000;
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
                            long s = SystemClock.millisClock().now();

                            String result = service.hello(data);

                            if (logger.isDebugEnabled()) {
                                logger.debug(result + " time cost=" + (SystemClock.millisClock().now() - s));
                            }
                            if (count.getAndIncrement() % 5000 == 0) {
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
        System.err.println("Request count: " + count.get() + ", time: " + second + " second, qps: " + count.get() / second);
    }
}
