package org.jupiter.rpc;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JFilterChain<T> {

    JFilter<T> getFilter();

    JFilterChain<T> getNext();

    void doFilter(JRequest request, JFilterContext<T> filterCtx) throws Throwable;
}
