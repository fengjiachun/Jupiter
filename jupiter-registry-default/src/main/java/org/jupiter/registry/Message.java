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

package org.jupiter.registry;

import java.util.concurrent.atomic.AtomicLong;

import static org.jupiter.serialization.SerializerHolder.defaultSerializerCode;

/**
 * 发布订阅信息的包装类.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class Message {

    private static final AtomicLong sequenceGenerator = new AtomicLong(0);

    private final long sequence;
    private byte messageCode;
    private byte serializerCode = defaultSerializerCode();
    private long version; // 版本号
    private Object data;

    public Message() {
        this(sequenceGenerator.getAndIncrement());
    }

    public Message(long sequence) {
        this.sequence = sequence;
    }

    public long sequence() {
        return sequence;
    }

    public byte messageCode() {
        return messageCode;
    }

    public void messageCode(byte messageCode) {
        this.messageCode = messageCode;
    }

    public byte serializerCode() {
        return serializerCode;
    }

    public void serializerCode(byte serializerCode) {
        this.serializerCode = serializerCode;
    }

    public long version() {
        return version;
    }

    public void version(long version) {
        this.version = version;
    }

    public Object data() {
        return data;
    }

    public void data(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sequence=" + sequence +
                ", messageCode=" + messageCode +
                ", serializerCode=" + serializerCode +
                ", version=" + version +
                ", data=" + data +
                '}';
    }
}
