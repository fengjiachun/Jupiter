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

package org.jupiter.transport.payload;

/**
 * 消息体bytes载体, 避免在IO线程中序列化/反序列化, jupiter-transport这一层不关注消息体的对象结构.
 *
 * 在jupiter框架内只有一种情况下会导致不同线程对 {@code BytesHolder} 读/写, 就是transport层decoder会将数据写进
 * {@code BytesHolder} 封装到 {@code Runnable} 中并提交到 {@code Executor}, 但这并不会导致内存一致性相关问题,
 * 因为线程将 {@code Runnable} 对象提交到 {@code Executor} 之前的操作 happen-before 其执行开始;
 *
 * Memory consistency effects:
 *    Actions in a thread prior to submitting a {@code Runnable} object({@code BytesHolder} object in it)
 *    to an {@code Executor} happen-before its execution begins, perhaps in another thread.
 *
 * jupiter
 * org.jupiter.transport.payload
 *
 * @author jiachun.fjc
 */
public abstract class BytesHolder {

    private byte serializerCode;
    private byte[] bytes;

    public byte serializerCode() {
        return serializerCode;
    }

    public byte[] bytes() {
        return bytes;
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        this.serializerCode = serializerCode;
        this.bytes = bytes;
    }

    public void nullBytes() {
        bytes = null; // help gc
    }

    public int size() {
        return bytes == null ? 0 : bytes.length;
    }
}
