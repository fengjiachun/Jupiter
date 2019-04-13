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
package org.jupiter.tracing;

import java.util.Iterator;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

/**
 *
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
public interface TracerFactory {

    TracerFactory DEFAULT = new DefaultTracerFactory();

    /**
     * Get a {@link Tracer} implementation.
     */
    Tracer getTracer();

    class DefaultTracerFactory implements TracerFactory {

        private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultTracerFactory.class);

        private static Tracer tracer = loadTracer();

        private static Tracer loadTracer() {
            try {
                Iterator<Tracer> implementations = JServiceLoader.load(Tracer.class).iterator();
                if (implementations.hasNext()) {
                    Tracer first = implementations.next();
                    if (!implementations.hasNext()) {
                        return first;
                    }

                    logger.warn("More than one tracer is found, NoopTracer will be used as default.");

                    return NoopTracerFactory.create();
                }
            } catch (Throwable t) {
                logger.error("Load tracer failed: {}.", StackTraceUtil.stackTrace(t));
            }
            return NoopTracerFactory.create();
        }

        @Override
        public Tracer getTracer() {
            return tracer;
        }
    }
}
