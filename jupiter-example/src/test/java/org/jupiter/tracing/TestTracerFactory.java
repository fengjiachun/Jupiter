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

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * jupiter
 * org.jupiter.tracing
 *
 * @author jiachun.fjc
 */
public class TestTracerFactory implements TracerFactory {

    private final Tracing tracing = Tracing.newBuilder()
            .spanReporter(AsyncReporter.create(URLConnectionSender.create("http://127.0.0.1:9411")))
            .build();

    private final BraveTracer braveTracer = BraveTracer.newBuilder(tracing)
            .build();

    @Override
    public Tracer getTracer() {
        return braveTracer;
    }
}
