package org.jupiter.monitor;

import org.jupiter.common.util.Maps;

import java.util.Map;

/**
 * jupiter
 * org.jupiter.monitor
 *
 * @author jiachun.fjc
 */
public enum CommandType {
    AUTH("Login with password"),
    HELP("Help information"),
    QUIT("Quit monitor")
    ;

    private String description;

    public String description() {
        return description;
    }

    CommandType(String description) {
        this.description = description;
    }

    public static CommandType parse(String name) {
        return commands.get(name.toLowerCase());
    }

    private static final Map<String, CommandType> commands = Maps.newHashMap();
    static {
        for (CommandType c : CommandType.values()) {
            commands.put(c.name().toLowerCase(), c);
        }
    }
}
