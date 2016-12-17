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

import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.transport.Status;
import org.jupiter.transport.payload.JResponseBytes;

/**
 * Provider's response data.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JResponse {

    private final JResponseBytes responseBytes; // 响应bytes[]
    private ResultWrapper result;               // 服务调用结果

    public JResponse(long id) {
        responseBytes = new JResponseBytes(id);
    }

    public JResponse(JResponseBytes responseBytes) {
        this.responseBytes = responseBytes;
    }

    public JResponseBytes responseBytes() {
        return responseBytes;
    }

    public long id() {
        return responseBytes.id();
    }

    public byte status() {
        return responseBytes.status();
    }

    public void status(byte status) {
        responseBytes.status(status);
    }

    public void status(Status status) {
        responseBytes.status(status.value());
    }

    public byte serializerCode() {
        return responseBytes.serializerCode();
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        responseBytes.bytes(serializerCode, bytes);
    }

    public ResultWrapper result() {
        return result;
    }

    public void result(ResultWrapper result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "JResponse{" +
                "status=" + Status.parse(status()) +
                ", id=" + id() +
                ", result=" + result +
                '}';
    }
}
