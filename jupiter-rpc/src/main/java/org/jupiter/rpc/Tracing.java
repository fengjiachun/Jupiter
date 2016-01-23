/*
 * Copyright (c) 2016 The Jupiter Project
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

package org.jupiter.rpc;

import org.jupiter.common.util.IPv4Util;
import org.jupiter.common.util.StringBuilderHelper;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.SystemPropertyUtil;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class Tracing {

    private static final boolean IS_TRACING_NEEDED = SystemPropertyUtil.getBoolean("jupiter.tracing.needed", true);

    private static final ThreadLocal<String> traceThreadLocal = new ThreadLocal<>();

    private static final char PID_FLAG = 'd';
    private static final String IP_16;
    private static final String PID;
    private static final int MIN_ID = 1000;
    private static final int MAX_ID = 9000;
    private static final AtomicInteger id = new AtomicInteger(MIN_ID);

    static {
        String _ip_16;
        try {
            _ip_16 = getIP_16(SystemPropertyUtil.get("jupiter.address", IPv4Util.getLocalAddress()));
        } catch (Exception e) {
            _ip_16 = "ffffffff";
        }
        IP_16 = _ip_16;

        String _pid;
        try {
            _pid = getHexPid(getPid());
        } catch (Exception e) {
            _pid = "0000";
        }
        PID = _pid;
    }

    public static String generateTraceId() {
        if (IS_TRACING_NEEDED) {
            return getTraceId(IP_16, SystemClock.millisClock().now(), getNextId());
        }
        return null;
    }

    public static void setCurrent(String traceId) {
        traceThreadLocal.set(traceId);
    }

    public static String getCurrent() {
        return traceThreadLocal.get();
    }

    private static String getHexPid(int pid) {
        // unsigned short 0 to 65535
        if (pid < 0) {
            pid = 0;
        }
        if (pid > 65535) {
            String strPid = Integer.toString(pid);
            strPid = strPid.substring(strPid.length() - 4, strPid.length());
            pid = Integer.parseInt(strPid);
        }
        String str = Integer.toHexString(pid);
        while (str.length() < 4) {
            str = "0" + str;
        }
        return str;
    }

    /**
     * Gets current pid, max pid 32 bit systems 32768, for 64 bit 4194304
     * http://unix.stackexchange.com/questions/16883/what-is-the-maximum-value-of-the-pid-of-a-process
     * http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
     */
    private static int getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int pid;
        try {
            pid = Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            pid = 0;
        }
        return pid;
    }

    private static String getIP_16(String ip) {
        String[] segments = ip.split("\\.");
        StringBuilder buf = StringBuilderHelper.get();
        for (String s : segments) {
            String hex = Integer.toHexString(Integer.parseInt(s));
            if (hex.length() == 1) {
                buf.append('0');
            }
            buf.append(hex);
        }
        return buf.toString();
    }

    private static String getTraceId(String ip_16, long timestamp, int nextId) {
        StringBuilder buf = StringBuilderHelper.get()
                .append(ip_16)
                .append(timestamp)
                .append(nextId)
                .append(PID_FLAG)
                .append(PID);
        return buf.toString();
    }

    private static int getNextId() {
        for (;;) {
            int current = id.get();
            int next = (current > MAX_ID) ? MIN_ID : current + 1;
            if (id.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
