package org.jupiter.rpc.provider.processor;

import org.jupiter.rpc.*;
import org.jupiter.rpc.provider.processor.task.MessageTask;

/**
 *
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public final class ProviderProcessorChains {

    private static JFilterChain<MessageTask.Context> headChain;

    static {
        JFilterChain<MessageTask.Context> tailChain = new DefaultFilterChain<>(new MessageTask.InvokeFilter(), null);
        headChain = new DefaultFilterChain<>(new MessageTask.InterceptorsFilter(), tailChain);
    }

    public static void doFilter(JRequest request, JFilterContext<MessageTask.Context> filterCtx) throws Throwable {
        headChain.doFilter(request, filterCtx);
    }

    public static void addFirst(JFilter<MessageTask.Context> filter) {
        headChain = new DefaultFilterChain<>(filter, headChain);
    }

    private ProviderProcessorChains() {}
}
