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

package org.jupiter.transport.netty.handler.connector;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.JResponse;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.consumer.processor.ConsumerProcessor;
import org.jupiter.common.util.Signal;
import org.jupiter.transport.exception.IoSignals;
import org.jupiter.transport.netty.channel.NettyChannel;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.transport.netty.handler.connector
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class ConnectorHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectorHandler.class);

    private final ConsumerProcessor processor;

    public ConnectorHandler(ConsumerProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof JResponse) {
            JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
            try {
                processor.handleResponse(jChannel, (JResponse) msg);
            } catch (Throwable t) {
                logger.error("An exception has been caught {}, on {} #channelRead().", t, jChannel);
            }
        } else {
            logger.warn("Unexpected message type received: {}.", msg.getClass());

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();

        // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
        // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
        if (!ch.isWritable()) {
            // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
            logger.warn("{} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                    ch, ch.config().getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());

            ch.config().setAutoRead(false);
        } else {
            // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
            logger.warn("{} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                    ch, ch.config().getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());

            ch.config().setAutoRead(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
        if (cause instanceof Signal) {
            IoSignals.handleSignal((Signal) cause, jChannel);
        } else {
            logger.error("An exception has been caught {}, on {}.", stackTrace(cause), jChannel);
        }
    }
}
