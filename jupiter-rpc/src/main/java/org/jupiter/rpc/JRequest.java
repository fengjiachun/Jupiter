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

import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.transport.payload.JRequestBytes;

/**
 * Consumer's request data.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JRequest {

    private final JRequestBytes requestBytes;   // 请求bytes[]
    private MessageWrapper message;             // 请求数据

    public JRequest() {
        this(new JRequestBytes());
    }

    public JRequest(JRequestBytes requestBytes) {
        this.requestBytes = requestBytes;
    }

    public JRequestBytes requestBytes() {
        return requestBytes;
    }

    public long invokeId() {
        return requestBytes.invokeId();
    }

    public long timestamp() {
        return requestBytes.timestamp();
    }

    public void timestamp(long timestamp) {
        requestBytes.timestamp(timestamp);
    }

    public byte serializerCode() {
        return requestBytes.serializerCode();
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        requestBytes.bytes(serializerCode, bytes);
    }

    public MessageWrapper message() {
        return message;
    }

    public void message(MessageWrapper message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "JRequest{" +
                "invokeId=" + invokeId() +
                ", timestamp=" + timestamp() +
                ", serializerCode=" + serializerCode() +
                ", message=" + message +
                '}';
    }
}
