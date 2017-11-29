package org.jupiter.rpc.tracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import org.jupiter.common.util.internal.UnsafeIntegerFieldUpdater;
import org.jupiter.common.util.internal.UnsafeUpdater;
import org.jupiter.rpc.JFilter;
import org.jupiter.rpc.JFilterChain;
import org.jupiter.rpc.JFilterContext;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.model.metadata.MessageWrapper;

/**
 *
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
public class OpenTracingFilter<T> implements JFilter<T> {

    private static final UnsafeIntegerFieldUpdater<TraceId> traceNodeUpdater =
            UnsafeUpdater.newIntegerFieldUpdater(TraceId.class, "node");

    private final Tracer tracer = TracerFactory.DEFAULT.getTracer();

    @Override
    public void doFilter(JRequest request, JFilterContext<T> filterCtx, JFilterChain<T> next) throws Throwable {
        if (TracingUtil.isTracingNeeded()) {
            bindCurrentTraceId(request.message().getTraceId());
        }

        try {
            if (tracer instanceof NoopTracer) {
                next.doFilter(request, filterCtx);
            } else {
                processTracing(request, filterCtx, next);
            }
        } finally {
            if (TracingUtil.isTracingNeeded()) {
                TracingUtil.clearCurrent();
            }
        }
    }

    private void processTracing(JRequest request, JFilterContext<T> filterCtx, JFilterChain<T> next) throws Throwable {
        MessageWrapper msg = request.message();
        TraceId traceId = TracingUtil.getCurrent();

        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(msg.getOperationName());
        Span span = spanBuilder.startManual();
        try {
            span.setTag("traceId", traceId.getId());
            next.doFilter(request, filterCtx);
        } finally {
            span.finish();
        }
    }

    private static void bindCurrentTraceId(TraceId traceId) {
        if (traceId != null) {
            assert traceNodeUpdater != null;
            traceNodeUpdater.set(traceId, traceId.getNode() + 1);
        }
        TracingUtil.setCurrent(traceId);
    }
}
