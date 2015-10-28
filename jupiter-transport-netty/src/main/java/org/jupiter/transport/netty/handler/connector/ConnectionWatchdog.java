package org.jupiter.transport.netty.handler.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.transport.netty.channel.NettyChannel;
import org.jupiter.transport.netty.handler.ChannelHandlerHolder;

import java.util.concurrent.TimeUnit;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * Connections watchdog.
 *
 * jupiter
 * org.jupiter.transport.netty.handler.connector
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionWatchdog.class);

    private final Bootstrap bootstrap;
    private final Timer timer;
    private final UnresolvedAddress remoteAddress;
    private final JChannelGroup group;

    private volatile boolean reconnect = true;
    private int attempts;

    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, UnresolvedAddress remoteAddress, JChannelGroup group) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.remoteAddress = remoteAddress;
        this.group = group;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel nChannel = NettyChannel.attachChannel(ctx.channel());
        group.add(nChannel);
        attempts = 0;

        logger.info("Connects with {}.", nChannel);

        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        boolean doReconnect = reconnect;
        if (doReconnect) {
            if (attempts < 12) {
                attempts++;
            }
            int timeout = 2 << attempts;
            timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
        }

        logger.warn("Disconnects with {}, address: [{}:{}], reconnect: {}.",
                ctx.channel(), remoteAddress.getHost(), remoteAddress.getPort(), doReconnect);

        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel ch = ctx.channel();

        logger.error("An exception has been caught {}, on {}.", stackTrace(cause), ch);

        ch.close();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        ChannelFuture future;
        final String host = remoteAddress.getHost();
        final int port = remoteAddress.getPort();
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(host, port);
        }

        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                boolean succeed = f.isSuccess();
                Channel ch = f.channel();

                logger.warn("Reconnects with [{}:{}] {}.", host, port, succeed ? "succeed" : "failed");

                if (!succeed) {
                    ch.pipeline().fireChannelInactive();
                }
            }
        });
    }
}
