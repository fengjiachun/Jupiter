package org.jupiter.tracing;

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
 * org.jupiter.tracing
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

    private <T> void processTracing(JRequest request, T context, JFilterChain next) throws Throwable {
        MessageWrapper msg = request.message();
        TraceId traceId = msg.getTraceId();

        if (traceId == null) {
            next.doFilter(request, context);
            return;
        }

        // TODO 还没写完 :)
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(msg.getOperationName());
        Span span = spanBuilder.startManual();
        try {
            span.setTag("traceId", traceId.getId());
            next.doFilter(request, context);
        } finally {
            span.finish();
        }
    }
}
