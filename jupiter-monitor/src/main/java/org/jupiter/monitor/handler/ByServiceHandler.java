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
public class ByServiceHandler extends ChildCommandHandler<RegistryHandler> {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        RegistryMonitor monitor = getParent().getMonitor();
        if (monitor == null) {
            return;
        }

        if (args.length < 5) {
            channel.writeAndFlush("Args[2]: group, args[3]: version, args[4]: serviceProviderName" + NEWLINE);
            return;
        }
        Command.ChildCommand childGrep = null;
        if (args.length >= 7) {
            childGrep = command.parseChild(args[5]);
        }

        for (String a : monitor.listAddressesByService(args[2], args[3], args[4])) {
            if (childGrep != null && childGrep == Command.ChildCommand.GREP) {
                if (a.contains(args[6])) {
                    channel.writeAndFlush(a + NEWLINE);
                }
            } else {
                channel.writeAndFlush(a + NEWLINE);
            }
        }
    }
}
