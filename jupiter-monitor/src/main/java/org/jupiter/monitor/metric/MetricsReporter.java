package org.jupiter.monitor.metric;

import com.codahale.metrics.ConsoleReporter;
import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.rpc.metric.Metrics;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
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
        synchronized (reporter) {
            reporter.report();
            return consoleOutput();
        }
    }

    private static String consoleOutput() {
        synchronized (reporter) {
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
