package org.jupiter.monitor.handler;

import io.netty.channel.Channel;
import org.jupiter.monitor.Command;
import org.jupiter.registry.RegistryMonitor;

import static org.jupiter.common.util.JConstants.NEWLINE;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class ByAddressHandler extends ChildCommandHandler<RegistryHandler> {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        RegistryMonitor monitor = getParent().getMonitor();
        if (monitor == null) {
            return;
        }

        if (args.length < 4) {
            channel.writeAndFlush("Args[2]: host, args[3]: port" + NEWLINE);
            return;
        }
        Command.ChildCommand childGrep = null;
        if (args.length >= 6) {
            childGrep = command.parseChild(args[4]);
        }

        for (String a : monitor.listServicesByAddress(args[2], Integer.parseInt(args[3]))) {
            if (childGrep != null && childGrep == Command.ChildCommand.GREP) {
                if (a.contains(args[5])) {
                    channel.writeAndFlush(a + NEWLINE);
                }
            } else {
                channel.writeAndFlush(a + NEWLINE);
            }
        }
    }
}
