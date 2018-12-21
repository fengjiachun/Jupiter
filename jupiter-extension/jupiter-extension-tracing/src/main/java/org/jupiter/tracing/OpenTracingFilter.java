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
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.jupiter.common.util.SpiMetadata;
import org.jupiter.rpc.JFilter;
import org.jupiter.rpc.JFilterChain;
import org.jupiter.rpc.JFilterContext;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.model.metadata.MessageWrapper;

import java.util.Iterator;
import java.util.Map;

/**
 * This filter enables distributed tracing in Jupiter clients and servers via
 * @see <a href="http://opentracing.io">The OpenTracing Project </a> : a set of consistent,
 * expressive, vendor-neutral APIs for distributed tracing and context propagation.
 *
 * 参考了以下代码:
 * <A>https://github.com/weibocom/motan/blob/master/motan-extension/filter-extension/filter-opentracing/src/main/java/com/weibo/api/motan/filter/opentracing/OpenTracingFilter.java</A>
 *
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
@SpiMetadata(name = "openTracing", priority = 10)
public class OpenTracingFilter implements JFilter {

    @Override
    public Type getType() {
        return Type.ALL;
    }

    @Override
    public <T extends JFilterContext> void doFilter(JRequest request, T filterCtx, JFilterChain next) throws Throwable {
        Tracer tracer = OpenTracingContext.getTracer();
        if (tracer == null || tracer instanceof NoopTracer) {
            next.doFilter(request, filterCtx);
            return;
        }

        Type filterCtxType = filterCtx.getType();
        if (filterCtxType == Type.PROVIDER) {
            processProviderTracing(tracer, request, filterCtx, next);
        } else if (filterCtxType == Type.CONSUMER) {
            processConsumerTracing(tracer, request, filterCtx, next);
        } else {
            throw new IllegalArgumentException("Illegal filter context type: " + filterCtxType);
        }
    }

    private <T extends JFilterContext> void processProviderTracing(
            Tracer tracer, JRequest request, T filterCtx, JFilterChain next) throws Throwable {
        Span span = extractContext(tracer, request);
        try {
            OpenTracingContext.setActiveSpan(span);
            // next filter
            next.doFilter(request, filterCtx);

            span.log("request success.");
        } catch (Throwable t) {
            span.log("request fail. " + t.getMessage());
            throw t;
        } finally {
            span.finish();
        }
    }

    private <T extends JFilterContext> void processConsumerTracing(
            Tracer tracer, JRequest request, T filterCtx, JFilterChain next) throws Throwable {
        MessageWrapper msg = request.message();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(msg != null ? msg.getOperationName() : "null");
        Span activeSpan = OpenTracingContext.getActiveSpan();
        if (activeSpan != null) {
            spanBuilder.asChildOf(activeSpan);
        }

        Span span = spanBuilder.start();
        try {
            injectContext(tracer, span, request);

            // next filter
            next.doFilter(request, filterCtx);

            span.log("request success.");
        } catch (Throwable t){
            span.log("request fail. " + t.getMessage());
            throw t;
        } finally {
            span.finish();
        }
    }

    private void injectContext(Tracer tracer, Span span, final JRequest request) {
        tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new TextMap() {

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("iterator");
            }

            @Override
            public void put(String key, String value) {
                request.putAttachment(key, value);
            }
        });
    }

    private Span extractContext(Tracer tracer, JRequest request) {
        MessageWrapper msg = request.message();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(msg != null ? msg.getOperationName() : "null");
        try {
            SpanContext spanContext = tracer.extract(
                    Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(request.getAttachments()));
            if (spanContext != null) {
                spanBuilder.asChildOf(spanContext);
            }
        } catch (Throwable t) {
            spanBuilder.withTag("Error", "extract from request failed: " + t.getMessage());
        }
        return spanBuilder.start();
    }
}
