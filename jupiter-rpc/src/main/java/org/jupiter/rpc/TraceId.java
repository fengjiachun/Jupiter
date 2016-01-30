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

package org.jupiter.rpc;

import org.jupiter.common.util.StringBuilderHelper;

import java.io.Serializable;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class TraceId implements Serializable {

    private static final long serialVersionUID = 2901824755629719770L;

    private final String id;
    private int node;

    public static TraceId newInstance(String id) {
        return new TraceId(id);
    }

    private TraceId(String id) {
        this.id = id;
        node = 0;
    }

    public String getId() {
        return id;
    }

    public int getNode() {
        return node;
    }

    public String asText() {
        StringBuilder buf = StringBuilderHelper.get()
                .append("TraceId{id='")
                .append(id)
                .append("', node=")
                .append(node)
                .append('}');
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TraceId traceId = (TraceId) o;

        return node == traceId.node && id.equals(traceId.id);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + node;
        return result;
    }

    @Override
    public String toString() {
        return "TraceId{" +
                "id='" + id + '\'' +
                ", node=" + node +
                '}';
    }
}
