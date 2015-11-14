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

/**
 * Response status.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public enum Status {

    OK(                         (byte) 0x20, "OK"),                             // 正常 - 请求已完成
    CLIENT_TIMEOUT(             (byte) 0x30, "CLIENT_TIMEOUT"),                 // 超时 - 客户端超时
    SERVER_TIMEOUT(             (byte) 0x31, "SERVER_TIMEOUT"),                 // 超时 - 服务端超时
    BAD_REQUEST(                (byte) 0x40, "BAD_REQUEST"),                    // 错误请求 — 请求中有语法问题, 或不能满足请求
    SERVICE_NOT_FOUND(          (byte) 0x44, "SERVICE_NOT_FOUND"),              // 找不到 - 指定服务不存在
    SERVER_ERROR(               (byte) 0x50, "SERVER_ERROR"),                   // 内部错误 — 因为意外情况, 服务器不能完成请求
    SERVER_BUSY(                (byte) 0x51, "SERVER_BUSY"),                    // 内部错误 — 服务器太忙, 无法处理新的请求
    SERVICE_ERROR(              (byte) 0x52, "SERVICE_ERROR"),                  // 服务错误 - 服务执行意外出错
    APP_SERVICE_TPS_LIMIT(      (byte) 0x53, "APP_SERVICE_TPS_LIMIT"),          // 服务错误 - App级别服务限流
    PROVIDER_SERVICE_TPS_LIMIT( (byte) 0x534, "PROVIDER_SERVICE_TPS_LIMIT");    // 服务错误 - Provider级别服务限流

    Status(byte value, String description) {
        this.value = value;
        this.description = description;
    }

    private byte value;
    private String description;

    public static Status parse(byte value) {
        for (Status s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        return null;
    }

    public byte value() {
        return value;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return description();
    }
}
