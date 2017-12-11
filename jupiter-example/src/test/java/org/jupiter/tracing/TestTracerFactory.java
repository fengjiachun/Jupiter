package org.jupiter.tracing;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;

/**
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
public class TestTracerFactory implements TracerFactory {

    private final Tracing tracing = Tracing.newBuilder()
            .build();

    private final BraveTracer braveTracer = BraveTracer.newBuilder(tracing)
            .build();

    @Override
    public Tracer getTracer() {
        return braveTracer;
    }
}
