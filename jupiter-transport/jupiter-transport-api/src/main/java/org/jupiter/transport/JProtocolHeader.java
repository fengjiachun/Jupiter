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

/**
 * Jupiter传输层协议头
 *
 * **************************************************************************************************
 *                                          Protocol
 *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *       2   │   1   │    1   │     8     │      4      │
 *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
 *           │       │        │           │             │
 *  │  MAGIC   Sign    Status   Invoke Id    Body Size                    Body Content              │
 *           │       │        │           │             │
 *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *
 * 消息头16个字节定长
 * = 2 // magic = (short) 0xbabe
 * + 1 // 消息标志位, 低地址4位用来表示消息类型request/response/heartbeat等, 高地址4位用来表示序列化类型
 * + 1 // 状态位, 设置请求响应状态
 * + 8 // 消息 id, long 类型, 未来jupiter可能将id限制在48位, 留出高地址的16位作为扩展字段
 * + 4 // 消息体 body 长度, int 类型
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class JProtocolHeader {

    /** 协议头长度 */
    public static final int HEADER_SIZE = 16;
    /** Magic */
    public static final short MAGIC = (short) 0xbabe;

    /** Message Code: 0x01 ~ 0x0f =================================================================================== */
    public static final byte REQUEST                    = 0x01;     // Request
    public static final byte RESPONSE                   = 0x02;     // Response
    public static final byte PUBLISH_SERVICE            = 0x03;     // 发布服务
    public static final byte PUBLISH_CANCEL_SERVICE     = 0x04;     // 取消发布服务
    public static final byte SUBSCRIBE_SERVICE          = 0x05;     // 订阅服务
    public static final byte OFFLINE_NOTICE             = 0x06;     // 通知下线
    public static final byte ACK                        = 0x07;     // Acknowledge
    public static final byte HEARTBEAT                  = 0x0f;     // Heartbeat

    private byte messageCode;       // sign 低地址4位

    /** Serializer Code: 0x01 ~ 0x0f ================================================================================ */
    // 位数限制最多支持15种不同的序列化/反序列化方式
    // protostuff   = 0x01
    // hessian      = 0x02
    // kryo         = 0x03
    // java         = 0x04
    // ...
    // XX1          = 0x0e
    // XX2          = 0x0f
    private byte serializerCode;    // sign 高地址4位
    private byte status;            // 响应状态码
    private long id;                // request.invokeId, 用于映射 <id, request, response> 三元组
    private int bodySize;           // 消息体长度

    public static byte toSign(byte serializerCode, byte messageCode) {
        return (byte) ((serializerCode << 4) | (messageCode & 0x0f));
    }

    public void sign(byte sign) {
        // sign 低地址4位
        this.messageCode = (byte) (sign & 0x0f);
        // sign 高地址4位, 先转成无符号int再右移4位
        this.serializerCode = (byte) ((((int) sign) & 0xff) >> 4);
    }

    public byte messageCode() {
        return messageCode;
    }

    public byte serializerCode() {
        return serializerCode;
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

    public int bodySize() {
        return bodySize;
    }

    public void bodySize(int bodyLength) {
        this.bodySize = bodyLength;
    }

    @Override
    public String toString() {
        return "JProtocolHeader{" +
                "messageCode=" + messageCode +
                ", serializerCode=" + serializerCode +
                ", status=" + status +
                ", id=" + id +
                ", bodySize=" + bodySize +
                '}';
    }
}
