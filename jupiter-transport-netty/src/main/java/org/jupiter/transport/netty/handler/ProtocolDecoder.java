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

package org.jupiter.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.jupiter.common.util.SystemClock;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.Response;
import org.jupiter.transport.JProtocolHeader;

import java.util.List;

import static org.jupiter.transport.JProtocolHeader.*;
import static org.jupiter.transport.error.Signals.ILLEGAL_MAGIC;
import static org.jupiter.transport.error.Signals.ILLEGAL_SIGN;

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
public class ProtocolDecoder extends ReplayingDecoder<ProtocolDecoder.State> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProtocolDecoder.class);

    public ProtocolDecoder() {
        super(State.HEADER);
    }

    // 协议头
    private final JProtocolHeader header = new JProtocolHeader();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Channel ch = ctx.channel();

        switch (state()) {
            case HEADER:
                ByteBuf buf = in.readSlice(HEAD_LENGTH);

                if (MAGIC != buf.readShort()) {     // MAGIC
                    throw ILLEGAL_MAGIC;
                }

                header.sign(buf.readByte());        // 消息标志位
                header.status(buf.readByte());      // 状态位
                header.id(buf.readLong());          // 消息id
                header.bodyLength(buf.readInt());   // 消息体长度

                checkpoint(State.BODY);
            case BODY:
                switch (header.sign()) {
                    case HEARTBEAT:

                        logger.debug("Heartbeat on channel {}.", ch);

                        break;
                    case REQUEST: {
                        byte[] bytes = new byte[header.bodyLength()];
                        in.readBytes(bytes);

                        Request request = new Request(header.id());
                        request.timestamps(SystemClock.millisClock().now());
                        request.bytes(bytes);
                        out.add(request);

                        logger.debug("Request [{}], on channel {}.", header, ch);

                        break;
                    }
                    case RESPONSE: {
                        byte[] bytes = new byte[header.bodyLength()];
                        in.readBytes(bytes);

                        Response response = new Response(header.id());
                        response.status(header.status());
                        response.bytes(bytes);
                        out.add(response);

                        logger.debug("Response [{}], on channel {}.", header, ch);

                        break;
                    }
                    default:
                        throw ILLEGAL_SIGN;
                }
                checkpoint(State.HEADER);
        }
    }

    enum State {
        HEADER,
        BODY
    }
}
