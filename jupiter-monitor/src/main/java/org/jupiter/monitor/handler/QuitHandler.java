package org.jupiter.monitor.handler;

import io.netty.channel.Channel;
import org.jupiter.monitor.Command;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static org.jupiter.common.util.JConstants.NEWLINE;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class QuitHandler implements CommandHandler {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        channel.writeAndFlush("Bye bye!" + NEWLINE).addListener(CLOSE);
    }
}
