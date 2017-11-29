package org.jupiter.rpc;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultFilterChain<T> implements JFilterChain<T> {

    private final JFilter<T> filter;
    private final JFilterChain<T> next;

    public DefaultFilterChain(JFilter<T> filter, JFilterChain<T> next) {
        this.filter = checkNotNull(filter, "filter");
        this.next = next;
    }

    @Override
    public void doFilter(JRequest request, JFilterContext<T> filterCtx) throws Throwable {
        filter.doFilter(request, filterCtx, next);
    }
}
