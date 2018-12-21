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
import org.jupiter.serialization.io.OutputBuf;
import org.jupiter.transport.payload.JRequestPayload;

import java.util.Collections;
import java.util.Map;

/**
 * Consumer's request data.
 *
 * 请求信息载体.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JRequest {

    private final JRequestPayload payload;   // 请求bytes/stream
    private MessageWrapper message;          // 请求对象

    public JRequest() {
        this(new JRequestPayload());
    }

    public JRequest(JRequestPayload payload) {
        this.payload = payload;
    }

    public JRequestPayload payload() {
        return payload;
    }

    public long invokeId() {
        return payload.invokeId();
    }

    public long timestamp() {
        return payload.timestamp();
    }

    public byte serializerCode() {
        return payload.serializerCode();
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        payload.bytes(serializerCode, bytes);
    }

    public void outputBuf(byte serializerCode, OutputBuf outputBuf) {
        payload.outputBuf(serializerCode, outputBuf);
    }

    public MessageWrapper message() {
        return message;
    }

    public void message(MessageWrapper message) {
        this.message = message;
    }

    public Map<String, String> getAttachments() {
        Map<String, String> attachments =
                message != null ? message.getAttachments() : null;
        return attachments != null ? attachments : Collections.<String, String>emptyMap();
    }

    public void putAttachment(String key, String value) {
        if (message != null) {
            message.putAttachment(key, value);
        }
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
