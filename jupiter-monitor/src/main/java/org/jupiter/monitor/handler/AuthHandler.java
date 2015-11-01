package org.jupiter.monitor.handler;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.jupiter.common.util.MD5Util;
import org.jupiter.common.util.SystemPropertyUtil;
import org.jupiter.monitor.Command;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static org.jupiter.common.util.JConstants.NEWLINE;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public class AuthHandler implements CommandHandler {

    private static final AttributeKey<Object> AUTH_KEY = AttributeKey.valueOf("Auth");
    private static final Object AUTH_OBJECT = new Object();
    private static final String DEFAULT_PASSWORD = "e10adc3949ba59abbe56e057f20f883e"; // 123456

    @Override
    public void handle(Channel channel, Command command, String... args) {
        if (args.length < 2) {
            channel.writeAndFlush("Need password!" + NEWLINE);
            return;
        }

        String password = SystemPropertyUtil.get("monitor.server.password");
        if (password == null) {
            password = DEFAULT_PASSWORD;
        }

        if (password.equals(MD5Util.getMD5(args[1]))) {
            channel.attr(AUTH_KEY).setIfAbsent(AUTH_OBJECT);
            channel.writeAndFlush("OK" + NEWLINE);
        } else {
            channel.writeAndFlush("Permission denied!" + NEWLINE).addListener(CLOSE);
        }
    }

    public static boolean checkAuth(Channel channel) {
        if (channel.attr(AUTH_KEY).get() == null) {
            channel.writeAndFlush("Permission denied" + NEWLINE).addListener(CLOSE);
            return false;
        }
        return true;
    }
}
