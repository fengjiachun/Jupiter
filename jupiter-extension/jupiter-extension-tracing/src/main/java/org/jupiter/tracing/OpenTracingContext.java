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

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
public class OpenTracingContext {

    private static final ThreadLocal<Span> spanThreadLocal = new ThreadLocal<>();

    // 默认通过SPI加载Tracer实现
    private static TracerFactory tracerFactory = TracerFactory.DEFAULT;

    /**
     * 手动设置Tracer实现
     */
    public static void setTracerFactory(TracerFactory tracerFactory) {
        OpenTracingContext.tracerFactory = tracerFactory;
    }

    public static Tracer getTracer() {
        return tracerFactory.getTracer();
    }

    public static Span getActiveSpan() {
        return spanThreadLocal.get();
    }

    public static void setActiveSpan(Span span) {
        spanThreadLocal.set(span);
    }
}
