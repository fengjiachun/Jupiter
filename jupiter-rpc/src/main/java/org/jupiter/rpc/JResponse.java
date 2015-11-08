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

import static org.jupiter.rpc.Status.OK;
import static org.jupiter.rpc.Status.parse;

/**
 * Provider response data.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JResponse extends BytesHolder {

    private byte status = OK.value();

    private long id; // invoke id
    private ResultWrapper result; // 服务调用结果

    public JResponse() {}

    public JResponse(long id) {
        this.id = id;
    }

    public byte status() {
        return status;
    }

    public void status(byte status) {
        this.status = status;
    }

    public long id() {
        return id;
    }

    public void id(long id) {
        this.id = id;
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
                "status=" + parse(status) +
                ", id=" + id +
                ", result=" + result +
                '}';
    }
}
