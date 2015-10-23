package org.jupiter.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class StackTraceUtil {

    public static String stackTrace(Throwable t) {
        if (t == null) {
            return "null";
        }
        StringWriter sw;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private StackTraceUtil() {}
}
