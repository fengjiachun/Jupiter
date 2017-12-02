package org.jupiter.tracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import org.jupiter.rpc.JFilter;
import org.jupiter.rpc.JFilterChain;
import org.jupiter.rpc.JFilterContext;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingUtil;

/**
 *
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
public class OpenTracingFilter<T> implements JFilter<T> {

    private final Tracer tracer = TracerFactory.DEFAULT.getTracer();

    @Override
    public void doFilter(JRequest request, JFilterContext<T> filterCtx, JFilterChain<T> next) throws Throwable {
        if (tracer instanceof NoopTracer) {
            next.doFilter(request, filterCtx);
        } else {
            processTracing(request, filterCtx, next);
        }
    }

    private void processTracing(JRequest request, JFilterContext<T> filterCtx, JFilterChain<T> next) throws Throwable {
        MessageWrapper msg = request.message();
        TraceId traceId = TracingUtil.getCurrent();

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
