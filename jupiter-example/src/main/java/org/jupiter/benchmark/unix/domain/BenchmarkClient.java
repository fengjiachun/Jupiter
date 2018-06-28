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

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.DefaultClient;
import org.jupiter.rpc.InvokeType;
import org.jupiter.rpc.JClient;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.consumer.future.InvokeFutureContext;
import org.jupiter.rpc.load.balance.LoadBalancerType;
import org.jupiter.serialization.SerializerType;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.UnresolvedDomainAddress;
import org.jupiter.transport.netty.JNettyDomainConnector;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * 飞行记录: -XX:+UnlockCommercialFeatures -XX:+FlightRecorder
 *
 * jupiter
 * org.jupiter.benchmark.unix.domain
 *
 * @author jiachun.fjc
 */
public class BenchmarkClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BenchmarkClient.class);

    public static void main(String[] args) {
//        SystemPropertyUtil.setProperty("jupiter.transport.codec.low_copy", "true");

        int processors = Runtime.getRuntime().availableProcessors();
        SystemPropertyUtil
                .setProperty("jupiter.executor.factory.consumer.core.workers", String.valueOf(processors << 1));
        SystemPropertyUtil.setProperty("jupiter.tracing.needed", "false");
        SystemPropertyUtil.setProperty("jupiter.use.non_blocking_hash", "true");
        SystemPropertyUtil
                .setProperty("jupiter.executor.factory.affinity.thread", "false");
        SystemPropertyUtil
                .setProperty("jupiter.executor.factory.consumer.factory_name", "forkJoin");

        final JClient client = new DefaultClient().withConnector(new JNettyDomainConnector(processors) {

//            @Override
//            protected ThreadFactory workerThreadFactory(String name) {
//                return new AffinityNettyThreadFactory(name);
//            }
        });

        UnresolvedAddress[] addresses = new UnresolvedAddress[processors];
        for (int i = 0; i < processors; i++) {
            addresses[i] = new UnresolvedDomainAddress(UnixDomainPath.PATH);
            client.connector().connect(addresses[i]);
        }

        if (SystemPropertyUtil.getBoolean("jupiter.test.async", true)) {
            futureCall(client, addresses, processors);
        } else {
            syncCall(client, addresses, processors);
        }
    }

    private static void syncCall(JClient client, UnresolvedAddress[] addresses, int processors) {
        final Service service = ProxyFactory.factory(Service.class)
                .version("1.0.0")
                .client(client)
                .serializerType(SerializerType.PROTO_STUFF)
                .loadBalancerType(LoadBalancerType.ROUND_ROBIN)
                .addProviderAddress(addresses)
                .newProxyInstance();

        for (int i = 0; i < 10000; i++) {
            try {
                service.hello("jupiter");
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
                            service.hello("jupiter");

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

    private static void futureCall(JClient client, UnresolvedAddress[] addresses, int processors) {
        final Service service = ProxyFactory.factory(Service.class)
                .version("1.0.0")
                .client(client)
                .invokeType(InvokeType.ASYNC)
                .serializerType(SerializerType.PROTO_STUFF)
                .loadBalancerType(LoadBalancerType.ROUND_ROBIN)
                .addProviderAddress(addresses)
                .newProxyInstance();

        for (int i = 0; i < 10000; i++) {
            try {
                service.hello("jupiter");
                InvokeFutureContext.future().getResult();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        final int t = 80000;
        long start = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(processors << 4);
        final AtomicLong count = new AtomicLong();
        final int futureSize = 80;
        for (int i = 0; i < (processors << 4); i++) {
            new Thread(new Runnable() {
                List<InvokeFuture<?>> futures = Lists.newArrayListWithCapacity(futureSize);
                @SuppressWarnings("all")
                @Override
                public void run() {
                    for (int i = 0; i < t; i++) {
                        try {
                            service.hello("jupiter");
                            futures.add(InvokeFutureContext.future());
                            if (futures.size() == futureSize) {
                                int fSize = futures.size();
                                for (int j = 0; j < fSize; j++) {
                                    try {
                                        futures.get(j).getResult();
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                    }
                                }
                                futures.clear();
                            }
                            if (count.getAndIncrement() % 10000 == 0) {
                                logger.warn("count=" + count.get());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!futures.isEmpty()) {
                        int fSize = futures.size();
                        for (int j = 0; j < fSize; j++) {
                            try {
                                futures.get(j).getResult();
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                        futures.clear();
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
