package org.jupiter.transport.netty.handler;

import io.netty.channel.ChannelHandler;

/**
 * jupiter
 * org.jupiter.transport.netty.handler
 *
 * @author jiachun.fjc
 */
public interface ChannelHandlerHolder {

    ChannelHandler[] handlers();
}
