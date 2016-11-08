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

package org.jupiter.transport;

import static org.jupiter.serialization.SerializerHolder.defaultSerializerCode;

/**
 * ACK确认
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class Acknowledge {

    public Acknowledge() {}

    public Acknowledge(long sequence) {
        this.sequence = sequence;
    }

    private long sequence; // ACK序号
    private byte serializerCode = defaultSerializerCode();

    public long sequence() {
        return sequence;
    }

    public void sequence(long sequence) {
        this.sequence = sequence;
    }

    public byte serializerCode() {
        return serializerCode;
    }

    public void serializerCode(byte serializerCode) {
        this.serializerCode = serializerCode;
    }
}
