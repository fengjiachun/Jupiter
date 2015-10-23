package org.jupiter.rpc;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public enum Status {

    OK(                 (byte) 0x20, "OK"),                         // 正常 - 请求已完成
    CLIENT_TIMEOUT(     (byte) 0x30, "CLIENT_TIMEOUT"),             // 超时 - 客户端超时
    SERVER_TIMEOUT(     (byte) 0x31, "SERVER_TIMEOUT"),             // 超时 - 服务端超时
    BAD_REQUEST(        (byte) 0x40, "BAD_REQUEST"),                // 错误请求 — 请求中有语法问题, 或不能满足请求
    SERVICE_NOT_FOUND(  (byte) 0x44, "SERVICE_NOT_FOUND"),          // 找不到 - 指定服务不存在
    SERVER_ERROR(       (byte) 0x50, "SERVER_ERROR"),               // 内部错误 — 因为意外情况, 服务器不能完成请求
    SERVER_BUSY(        (byte) 0x51, "SERVER_BUSY"),                // 内部错误 — 服务器太忙, 无法处理新的请求
    SERVICE_ERROR(      (byte) 0x52, "SERVICE_ERROR"),              // 服务错误 - 服务执行意外出错
    SERVICE_TPS_LIMIT(  (byte) 0x53, "SERVICE_TPS_LIMIT");          // 服务错误 - 服务限流

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
