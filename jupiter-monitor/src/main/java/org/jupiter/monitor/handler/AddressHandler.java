package org.jupiter.monitor.handler;

import io.netty.channel.Channel;
import org.jupiter.monitor.Command;
import org.jupiter.registry.RegistryMonitor;

import java.util.List;

import static org.jupiter.common.util.JConstants.NEWLINE;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class AddressHandler extends ChildCommandHandler<RegistryHandler> {

    @Override
    public void handle(Channel channel, Command command, String... args) {
        RegistryMonitor monitor = getParent().getMonitor();
        if (monitor == null) {
            return;
        }

        Command.ChildCommand target = command.parseChild(args[2]);
        if (target == null) {
            channel.writeAndFlush("Wrong args denied!" + NEWLINE);
            return;
        }

        List<String> addresses;
        switch (target) {
            case P:
                addresses = monitor.listPublisherHosts();

                break;
            case S:
                addresses = monitor.listSubscriberAddresses();

                break;
            default:
                return;
        }
        Command.ChildCommand childGrep = null;
        if (args.length >= 5) {
            childGrep = command.parseChild(args[3]);
        }
        for (String a : addresses) {
            if (childGrep != null && childGrep == Command.ChildCommand.GREP) {
                if (a.contains(args[4])) {
                    channel.writeAndFlush(a + NEWLINE);
                }
            } else {
                channel.writeAndFlush(a + NEWLINE);
            }
        }
    }
}
