package org.jupiter.rpc.consumer.dispatcher;

import org.jupiter.rpc.JClient;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.aop.ConsumerHook;

import java.util.List;

import static org.jupiter.common.util.JConstants.DEFAULT_TIMEOUT;

/**
 * jupiter
 * org.jupiter.rpc.consumer.dispatcher
 *
 * @author jiachun.fjc
 */
public abstract class AbstractDispatcher implements Dispatcher {

    protected final JClient connector;

    private List<ConsumerHook> hooks;
    private JListener listener;
    private int timeoutMills = DEFAULT_TIMEOUT;

    public AbstractDispatcher(JClient connector) {
        this.connector = connector;
    }

    @Override
    public List<ConsumerHook> getHooks() {
        return hooks;
    }

    @Override
    public void setHooks(List<ConsumerHook> hooks) {
        this.hooks = hooks;
    }

    @Override
    public JListener getListener() {
        return listener;
    }

    @Override
    public void setListener(JListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTimeoutMills() {
        return timeoutMills;
    }

    @Override
    public void setTimeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
    }
}
