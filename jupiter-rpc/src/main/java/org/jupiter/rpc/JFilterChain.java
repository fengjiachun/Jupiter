package org.jupiter.rpc;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JFilterChain<T> {

    void doFilter(JRequest request, JFilterContext<T> filterCtx) throws Throwable;
}
