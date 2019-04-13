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
package org.jupiter.common.concurrent.disruptor;

import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import com.lmax.disruptor.ExceptionHandler;

/**
 * Jupiter
 * org.jupiter.common.concurrent.disruptor
 *
 * @author jiachun.fjc
 */
public class LoggingExceptionHandler implements ExceptionHandler<Object> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(LoggingExceptionHandler.class);

    @Override
    public void handleEventException(Throwable ex, long sequence, Object event) {
        if (logger.isWarnEnabled()) {
            logger.warn("Exception processing: {} {}, {}.", sequence, event, StackTraceUtil.stackTrace(ex));
        }
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        if (logger.isWarnEnabled()) {
            logger.warn("Exception during onStart(), {}.", StackTraceUtil.stackTrace(ex));
        }
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        if (logger.isWarnEnabled()) {
            logger.warn("Exception during onShutdown(), {}.", StackTraceUtil.stackTrace(ex));
        }
    }
}
