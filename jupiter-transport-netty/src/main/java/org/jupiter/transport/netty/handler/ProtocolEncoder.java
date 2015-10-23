package org.jupiter.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import org.jupiter.common.util.Reflects;
import org.jupiter.rpc.BytesHolder;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.Response;

import static org.jupiter.transport.JProtocolHeader.*;

/**
 * 消息头16个字节定长
 * = 2 // MAGIC = (short) 0xbabe
 * + 1 // 消息标志位, 用来表示消息类型Request/Response/Heartbeat
 * + 1 // 状态位, 设置请求响应状态
 * + 8 // 消息 id long 类型
 * + 4 // 消息体body长度, int类型
 *
 * jupiter
 * org.jupiter.transport.netty.handler
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class ProtocolEncoder extends MessageToByteEncoder<BytesHolder> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BytesHolder msg, ByteBuf out) throws Exception {
        if (msg instanceof Request) {
            Request request = (Request) msg;
            byte[] bytes = request.bytes();
            out.writeShort(MAGIC)
                    .writeByte(REQUEST)
                    .writeByte(0x00)
                    .writeLong(request.invokeId())
                    .writeInt(bytes.length)
                    .writeBytes(bytes);
        } else if (msg instanceof Response) {
            Response response = (Response) msg;
            byte[] bytes = response.bytes();
            out.writeShort(MAGIC)
                    .writeByte(RESPONSE)
                    .writeByte(response.status())
                    .writeLong(response.id())
                    .writeInt(bytes.length)
                    .writeBytes(bytes);
        } else {
            try {
                throw new IllegalArgumentException(Reflects.simpleClassName(msg));
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }
}
