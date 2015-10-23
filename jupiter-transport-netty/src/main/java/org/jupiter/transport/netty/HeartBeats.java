package org.jupiter.transport.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jupiter.transport.JProtocolHeader;

/**
 * 心跳
 *
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class HeartBeats {

    private static final ByteBuf HEARTBEAT_BUF;

    static {
        ByteBuf buf = Unpooled.buffer(JProtocolHeader.HEAD_LENGTH);
        // 共享的心跳内容
        buf.writeShort(JProtocolHeader.MAGIC);
        buf.writeByte(JProtocolHeader.HEARTBEAT);
        buf.writeByte(0);
        buf.writeLong(0);
        buf.writeInt(0);
        HEARTBEAT_BUF = Unpooled.unmodifiableBuffer(Unpooled.unreleasableBuffer(buf));
    }

    /**
     * 获取共享的心跳内容
     */
    public static ByteBuf heartbeatContent() {
        return HEARTBEAT_BUF.duplicate();
    }
}
