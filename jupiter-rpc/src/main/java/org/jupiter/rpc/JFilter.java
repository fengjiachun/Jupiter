package org.jupiter.rpc;

/**
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JFilter {

    <T> void doFilter(JRequest request, T filterCtx, JFilterChain next) throws Throwable;
}
