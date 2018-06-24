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

package org.jupiter.flight.exec;

import org.jupiter.common.util.internal.UnsafeReferenceFieldUpdater;
import org.jupiter.common.util.internal.UnsafeUpdater;

import java.io.*;
import java.nio.channels.Channel;
import java.util.Properties;

/**
 * Hack {@link java.lang.System}.
 *
 * jupiter
 * org.jupiter.flight.exec
 *
 * @author jiachun.fjc
 */
public class HackSystem {

    public static final InputStream in = System.in;

    private static final UnsafeReferenceFieldUpdater<ByteArrayOutputStream, byte[]> bufUpdater =
            UnsafeUpdater.newReferenceFieldUpdater(ByteArrayOutputStream.class, "buf");

    private static ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);

    public static final PrintStream out = new PrintStream(buf);
    @SuppressWarnings("unused")
    public static final PrintStream err = out;

    public static String getBufString() {
        String value = buf.toString();
        synchronized (HackSystem.class) {
            assert bufUpdater != null;
            if (bufUpdater.get(buf).length > (1024 << 3)) {
                bufUpdater.set(buf, new byte[1024]);
            }
        }
        return value;
    }

    public static void clearBuf() {
        buf.reset();
    }

    public static void setIn(InputStream in) {
        System.setIn(in);
    }

    public static void setOut(PrintStream out) {
        System.setOut(out);
    }

    public static void setErr(PrintStream err) {
        System.setErr(err);
    }

    public static Console console() {
        return System.console();
    }

    public static Channel inheritedChannel() throws IOException {
        return System.inheritedChannel();
    }

    public static void setSecurityManager(final SecurityManager s) {
        System.setSecurityManager(s);
    }

    public static SecurityManager getSecurityManager() {
        return System.getSecurityManager();
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long nanoTime() {
        return System.nanoTime();
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    public static int identityHashCode(Object x) {
        return System.identityHashCode(x);
    }

    public static Properties getProperties() {
        return System.getProperties();
    }

    public static void setProperties(Properties props) {
        System.setProperties(props);
    }

    public static String getProperty(String key) {
        return System.getProperty(key);
    }

    public static String getProperty(String key, String def) {
        return System.getProperty(key, def);
    }

    public static String setProperty(String key, String value) {
        return System.setProperty(key, value);
    }

    public static String clearProperty(String key) {
        return System.clearProperty(key);
    }

    public static String getenv(String name) {
        return System.getenv(name);
    }

    public static java.util.Map<String, String> getenv() {
        return System.getenv();
    }

    public static void exit(int status) {
        System.exit(status);
    }

    public static void gc() {
        System.gc();
    }

    public static void runFinalization() {
        System.runFinalization();
    }

    @SuppressWarnings("deprecation")
    public static void runFinalizersOnExit(boolean value) {
        System.runFinalizersOnExit(value);
    }

    public static void load(String filename) {
        System.load(filename);
    }

    public static void loadLibrary(String libname) {
        System.loadLibrary(libname);
    }

    public static String mapLibraryName(String libname) {
        return System.mapLibraryName(libname);
    }
}
