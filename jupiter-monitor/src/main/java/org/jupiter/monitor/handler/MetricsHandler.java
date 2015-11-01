package org.jupiter.monitor.handler;

import io.netty.channel.Channel;
import org.jupiter.monitor.Command;
import org.jupiter.monitor.metric.MetricsReporter;

import static org.jupiter.common.util.JConstants.NEWLINE;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class MetricsHandler implements CommandHandler {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        if (AuthHandler.checkAuth(channel)) {
            if (args.length < 2) {
                channel.writeAndFlush("Need second arg!" + NEWLINE);
                return;
            }

            Command.ChildCommand child = command.parseChild(args[1]);
            if (child != null) {
                switch (child) {
                    case REPORT:
                        channel.writeAndFlush(MetricsReporter.report());

                        break;
                }
            } else {
                channel.writeAndFlush("Wrong args denied!" + NEWLINE);
            }
        }
    }
}
