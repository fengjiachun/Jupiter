package org.jupiter.rpc;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JFilter<T> {

    void doFilter(JRequest request, JFilterContext<T> filterCtx, JFilterChain<T> next) throws Throwable;
}
