package org.jupiter.monitor.handler;

/**
 * jupiter
 * org.jupiter.monitor.handler
 *
 * @author jiachun.fjc
 */
public abstract class ChildCommandHandler<T extends CommandHandler> implements CommandHandler {

    private volatile T parent;

    public T getParent() {
        return parent;
    }

    public void setParent(T parent) {
        this.parent = parent;
    }
}
