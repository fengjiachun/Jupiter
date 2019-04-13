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
package org.jupiter.rpc.metric;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.jupiter.common.util.ClassUtil;
import org.jupiter.common.util.JConstants;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;

import static org.jupiter.common.util.Requires.requireNotNull;

/**
 * 指标度量
 *
 * jupiter
 * org.jupiter.rpc.metric
 *
 * @author jiachun.fjc
 */
public class Metrics {

    private static final MetricRegistry metricRegistry = new MetricRegistry();
    private static final ScheduledReporter scheduledReporter;
    static {
        // 检查是否存在slf4j, 使用Metrics必须显式引入slf4j依赖
        ClassUtil.checkClass("org.slf4j.Logger",
                "Class[" + Metric.class.getName() + "] must rely on SL4J");

        if (JConstants.METRIC_CSV_REPORTER) {
            scheduledReporter = CsvReporter
                    .forRegistry(metricRegistry)
                    .build(new File(JConstants.METRIC_CSV_REPORTER_DIRECTORY));
        } else {
            ScheduledReporter _reporter;
            try {
                _reporter = Slf4jReporter
                        .forRegistry(metricRegistry)
                        .withLoggingLevel(Slf4jReporter.LoggingLevel.WARN)
                        .build();
            } catch (NoClassDefFoundError e) {
                // No Slf4j
                _reporter = ConsoleReporter.forRegistry(metricRegistry).build();
            }
            scheduledReporter = _reporter;
        }
        scheduledReporter.start(JConstants.METRIC_REPORT_PERIOD, TimeUnit.MINUTES);
    }

    /**
     * Return the global registry of metric instances.
     */
    public static MetricRegistry metricRegistry() {
        return metricRegistry;
    }

    /**
     * Return the {@link Meter} registered under this name; or create and register
     * a new {@link Meter} if none is registered.
     */
    public static Meter meter(String name) {
        return metricRegistry.meter(requireNotNull(name, "name"));
    }

    /**
     * Return the {@link Meter} registered under this name; or create and register
     * a new {@link Meter} if none is registered.
     */
    public static Meter meter(Class<?> clazz, String... names) {
        return metricRegistry.meter(MetricRegistry.name(clazz, names));
    }

    /**
     * Return the {@link Timer} registered under this name; or create and register
     * a new {@link Timer} if none is registered.
     */
    public static Timer timer(String name) {
        return metricRegistry.timer(requireNotNull(name, "name"));
    }

    /**
     * Return the {@link Timer} registered under this name; or create and register
     * a new {@link Timer} if none is registered.
     */
    public static Timer timer(Class<?> clazz, String... names) {
        return metricRegistry.timer(MetricRegistry.name(clazz, names));
    }

    /**
     * Return the {@link Counter} registered under this name; or create and register
     * a new {@link Counter} if none is registered.
     */
    public static Counter counter(String name) {
        return metricRegistry.counter(requireNotNull(name, "name"));
    }

    /**
     * Return the {@link Counter} registered under this name; or create and register
     * a new {@link Counter} if none is registered.
     */
    public static Counter counter(Class<?> clazz, String... names) {
        return metricRegistry.counter(MetricRegistry.name(clazz, names));
    }

    /**
     * Return the {@link Histogram} registered under this name; or create and register
     * a new {@link Histogram} if none is registered.
     */
    public static Histogram histogram(String name) {
        return metricRegistry.histogram(requireNotNull(name, "name"));
    }

    /**
     * Return the {@link Histogram} registered under this name; or create and register
     * a new {@link Histogram} if none is registered.
     */
    public static Histogram histogram(Class<?> clazz, String... names) {
        return metricRegistry.histogram(MetricRegistry.name(clazz, names));
    }

    private Metrics() {}
}
