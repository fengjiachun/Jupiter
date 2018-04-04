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

package org.jupiter.rpc.tracing;

import java.io.Serializable;

/**
 * Jupiter框架内部链路追踪ID, 全局唯一, 如果接入了OpenTracing实现, 这个通常就没什么用了.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class TraceId implements Serializable {

    private static final long serialVersionUID = 2901824755629719770L;

    public static final TraceId NULL_TRACE_ID = newInstance("null");

    private final String id;    // 全局唯一的ID
    private int node;           // 每经过一个节点, node的值会 +1

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
        return id + "_" + node;
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
