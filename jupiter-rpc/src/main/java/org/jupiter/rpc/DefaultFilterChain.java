package org.jupiter.rpc;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultFilterChain implements JFilterChain {

    private final JFilter filter;
    private final JFilterChain next;

    public DefaultFilterChain(JFilter filter, JFilterChain next) {
        this.filter = checkNotNull(filter, "filter");
        this.next = next;
    }

    @Override
    public JFilter getFilter() {
        return filter;
    }

    @Override
    public JFilterChain getNext() {
        return next;
    }

    @Override
    public <T extends JFilterContext> void doFilter(JRequest request, T filterCtx) throws Throwable {
        filter.doFilter(request, filterCtx, next);
    }
}
