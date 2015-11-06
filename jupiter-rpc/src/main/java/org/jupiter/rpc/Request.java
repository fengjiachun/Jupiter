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

import java.util.concurrent.atomic.AtomicLong;

import static org.jupiter.rpc.Status.*;

/**
 * Consumer request data.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class Request extends BytesHolder {

    private static final AtomicLong invokeIdGenerator = new AtomicLong(0);

    private final long invokeId;
    private MessageWrapper message; // 请求数据

    private transient long timestamp;
    private transient Status status = OK;

    public Request() {
        this(invokeIdGenerator.getAndIncrement());
    }

    public Request(long invokeId) {
        this.invokeId = invokeId;
    }

    public long invokeId() {
        return invokeId;
    }

    public MessageWrapper message() {
        return message;
    }

    public void message(MessageWrapper message) {
        this.message = message;
    }

    public long timestamp() {
        return timestamp;
    }

    public void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Status status() {
        return status;
    }

    public void status(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Request{" +
                "invokeId=" + invokeId +
                ", message=" + message +
                '}';
    }
}
