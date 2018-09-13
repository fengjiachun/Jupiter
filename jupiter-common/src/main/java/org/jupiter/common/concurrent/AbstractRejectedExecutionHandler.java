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

package org.jupiter.common.concurrent;

import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.JvmTools;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * Jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public abstract class AbstractRejectedExecutionHandler implements RejectedExecutionHandler {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRejectedExecutionHandler.class);

    protected final String threadPoolName;
    private final AtomicBoolean dumpNeeded;
    private final String dumpPrefixName;

    public AbstractRejectedExecutionHandler(String threadPoolName, boolean dumpNeeded, String dumpPrefixName) {
        this.threadPoolName = threadPoolName;
        this.dumpNeeded = new AtomicBoolean(dumpNeeded);
        this.dumpPrefixName = dumpPrefixName;
    }

    public void dumpJvmInfoIfNeeded() {
        if (dumpNeeded.getAndSet(false)) {
            String now = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String name = threadPoolName + "_" + now;
            FileOutputStream fileOutput = null;
            try {
                fileOutput = new FileOutputStream(new File(dumpPrefixName + "_dump_" + name + ".log"));

                List<String> stacks = JvmTools.jStack();
                for (String s : stacks) {
                    fileOutput.write(s.getBytes(JConstants.UTF8));
                }

                List<String> memoryUsages = JvmTools.memoryUsage();
                for (String m : memoryUsages) {
                    fileOutput.write(m.getBytes(JConstants.UTF8));
                }

                if (JvmTools.memoryUsed() > 0.9) {
                    JvmTools.jMap(dumpPrefixName + "_dump_" + name + ".bin", false);
                }
            } catch (Throwable t) {
                logger.error("Dump jvm info error: {}.", stackTrace(t));
            } finally {
                if (fileOutput != null) {
                    try {
                        fileOutput.close();
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}
