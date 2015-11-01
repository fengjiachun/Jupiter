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
public class RegistryHandler implements CommandHandler {

    private volatile RegistryMonitor monitor;

    public RegistryMonitor getMonitor() {
        return monitor;
    }

    public void setMonitor(RegistryMonitor monitor) {
        this.monitor = monitor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(Channel channel, Command command, String... args) {
        if (AuthHandler.checkAuth(channel)) {
            if (args.length < 3) {
                channel.writeAndFlush("Need more args!" + NEWLINE);
                return;
            }

            Command.ChildCommand child = command.parseChild(args[1]);
            if (child != null) {
                CommandHandler childHandler = child.handler();
                if (childHandler == null) {
                    return;
                }
                if (childHandler instanceof ChildCommandHandler) {
                    if (((ChildCommandHandler) childHandler).getParent() == null) {
                        ((ChildCommandHandler) childHandler).setParent(this);
                    }
                }
                childHandler.handle(channel, command, args);
            } else {
                channel.writeAndFlush("Wrong args denied!" + NEWLINE);
            }
        }
    }
}
