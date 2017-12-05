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

package org.jupiter.tracing.api;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import org.jupiter.common.util.SpiImpl;
import org.jupiter.rpc.JFilter;
import org.jupiter.rpc.JFilterChain;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.tracing.TraceId;

/**
 *
 * jupiter
 * org.jupiter.tracing.api
 *
 * @author jiachun.fjc
 */
@SpiImpl(name = "openTracing", sequence = 10)
public class OpenTracingFilter implements JFilter {

    private final Tracer tracer = TracerFactory.DEFAULT.getTracer();

    @Override
    public <T> void doFilter(JRequest request, T filterCtx, JFilterChain next) throws Throwable {
        if (tracer instanceof NoopTracer) {
            next.doFilter(request, filterCtx);
        } else {
            processTracing(request, filterCtx, next);
        }
    }

    private <T> void processTracing(JRequest request, T filterCtx, JFilterChain next) throws Throwable {
        MessageWrapper msg = request.message();
        TraceId traceId = msg.getTraceId();

        if (traceId == null) {
            next.doFilter(request, filterCtx);
            return;
        }

        // TODO 还没写完 :)
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(msg.getOperationName());
        Span span = spanBuilder.startManual();
        try {
            span.setTag("traceId", traceId.getId());
            next.doFilter(request, filterCtx);
        } finally {
            span.finish();
        }
    }
}
