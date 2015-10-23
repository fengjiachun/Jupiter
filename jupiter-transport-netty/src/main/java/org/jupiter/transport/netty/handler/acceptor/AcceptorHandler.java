package org.jupiter.transport.netty.handler.acceptor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.provider.processor.ProviderProcessor;
import org.jupiter.transport.error.Signal;
import org.jupiter.transport.error.Signals;
import org.jupiter.transport.netty.channel.NettyChannel;

import java.util.concurrent.atomic.AtomicInteger;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.transport.netty.handler.acceptor
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class AcceptorHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AcceptorHandler.class);

    private static final AtomicInteger channelCounter = new AtomicInteger(0);

    private final ProviderProcessor processor;

    public AcceptorHandler(ProviderProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Request) {
            JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
            Request request = (Request) msg;
            try {
                processor.handleRequest(jChannel, request);
            } catch (Exception e) {
                processor.handleException(jChannel, request, e);
            }
        } else {
            logger.warn("Unexpected msg type received:{}.", msg.getClass());

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.incrementAndGet();

        logger.info("Connects with {} as the {}th channel.", ctx.channel(), count);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.getAndDecrement();

        logger.warn("Disconnects with {} as the {}th channel.", ctx.channel(), count);

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();

        /**
         * 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK (默认值 64 * 1024)
         * 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK (默认值 32 * 1024)
         */
        if (!ch.isWritable()) {
            // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
            if (ch.unsafe().outboundBuffer().size() > 32) {
                // 大于32块的小块数据, 网络可能真的不好, OS的snd_buf被填满, 一直不能flush, 关闭当前channel
                logger.error("{} is not writable. Going to close channel.", ch);

                ch.close();
            } else {
                // 可能是单次写入的数据比较大, 不关闭连接
                logger.warn("{} is not writable.", ch);
            }
        } else {
            // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
            logger.info("{} is writable.", ch);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        JChannel jChannel = NettyChannel.attachChannel(ctx.channel());
        if (cause instanceof Signal) {
            Signals.handleSignal((Signal) cause, jChannel);
        } else {
            logger.error("An exception has been caught {}, on {}.", stackTrace(cause), jChannel);
        }
    }
}
