package org.jupiter.monitor.handler;

import io.netty.channel.Channel;
import org.jupiter.monitor.Command;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public interface CommandHandler {

    void handle(Channel channel, Command command, String... args);
}
