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
package org.jupiter.serialization;

/**
 * 最多可以扩展到15种不同的序列化/反序列化方式, 取值范围为: 0x01 ~ 0x0f,
 * 每当想增加一种序列化/反序列化方式都要修改这个枚举, 不过我想这个限制是值得的.
 *
 * jupiter
 * org.jupiter.serialization
 *
 * @author jiachun.fjc
 */
public enum SerializerType {
    PROTO_STUFF ((byte) 0x01),
    HESSIAN     ((byte) 0x02),
    KRYO        ((byte) 0x03),
    JAVA        ((byte) 0x04)
    // ...
    ;

    SerializerType(byte value) {
        if (0x00 < value && value < 0x0f) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Out of range(0x01 ~ 0x0f): " + value);
        }
    }

    private final byte value;

    public byte value() {
        return value;
    }

    public static SerializerType parse(String name) {
        for (SerializerType s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    public static SerializerType parse(byte value) {
        for (SerializerType s : values()) {
            if (s.value() == value) {
                return s;
            }
        }
        return null;
    }

    public static SerializerType getDefault() {
        return PROTO_STUFF;
    }
}
