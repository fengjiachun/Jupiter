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

import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求的消息体bytes载体, 避免在IO线程中序列化/反序列化, jupiter-transport这一层不关注消息体的对象结构.
 *
 * jupiter
 * org.jupiter.transport.payload
 *
 * @author jiachun.fjc
 */
public class JRequestBytes extends BytesHolder {

    // 请求id自增器, 用于映射 <id, request, response> 三元组
    //
    // id在 <request, response> 生命周期内保证进程内唯一即可, 在Id对应的Response被处理完成后这个id就可以再次使用了,
    // 所以id可在 <Long.MIN_VALUE ~ Long.MAX_VALUE> 范围内从小到大循环利用, 即使溢出也是没关系的, 并且只是从理论上
    // 才有溢出的可能, 比如一个100万qps的系统把 <0 ~ Long.MAX_VALUE> 范围内的id都使用完大概需要29万年.
    //
    // 未来jupiter可能将invokeId限制在48位, 留出高地址的16位作为扩展字段.
    private static final AtomicLong sequence = new AtomicLong();

    // 用于映射 <id, request, response> 三元组
    private final long invokeId;
    // jupiter-transport层会在协议解析完成后打上一个时间戳, 用于后续监控对该请求的处理时间
    private transient long timestamp;

    public JRequestBytes() {
        this(sequence.incrementAndGet());
    }

    public JRequestBytes(long invokeId) {
        this.invokeId = invokeId;
    }

    public long invokeId() {
        return invokeId;
    }

    public long timestamp() {
        return timestamp;
    }

    public void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
