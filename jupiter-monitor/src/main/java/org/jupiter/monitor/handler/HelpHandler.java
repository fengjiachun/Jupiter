package org.jupiter.monitor.handler;

import io.netty.channel.Channel;
import org.jupiter.monitor.Command;

import static org.jupiter.common.util.JConstants.NEWLINE;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class HelpHandler implements CommandHandler {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        StringBuilder buf = new StringBuilder();
        buf.append("-- Help ------------------------------------------------------------------------")
                .append(NEWLINE);
        for (Command parent : Command.values()) {
            buf.append(String.format("%1$-32s", parent.name()))
                    .append(parent.description())
                    .append(NEWLINE);

            for (Command.ChildCommand child : parent.children()) {
                buf.append(String.format("%1$36s", "-"))
                        .append(child.name())
                        .append(' ')
                        .append(child.description())
                        .append(NEWLINE);
            }

            buf.append(NEWLINE);
        }
        channel.writeAndFlush(buf.toString());
    }
}
