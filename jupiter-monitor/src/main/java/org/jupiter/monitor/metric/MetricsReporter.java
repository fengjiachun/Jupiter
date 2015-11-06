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

package org.jupiter.monitor.metric;

import com.codahale.metrics.ConsoleReporter;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.rpc.metric.Metrics;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Indicators measure used to provide data for the monitor.
 *
 * jupiter
 * org.jupiter.monitor.metric
 *
 * @author jiachun.fjc
 */
public class MetricsReporter {

    private static final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private static final PrintStream output = new PrintStream(bytes);
    private static final ConsoleReporter reporter = ConsoleReporter.forRegistry(Metrics.metricRegistry())
                                                            .outputTo(output)
                                                            .build();

    public static String report() {
        synchronized (MetricsReporter.class) {
            reporter.report();
            return consoleOutput();
        }
    }

    private static String consoleOutput() {
        synchronized (MetricsReporter.class) {
            String output;
            try {
                output = bytes.toString(JConstants.UTF8_CHARSET);
            } catch (UnsupportedEncodingException e) {
                output = StackTraceUtil.stackTrace(e);
            }
            return output;
        }
    }
}
