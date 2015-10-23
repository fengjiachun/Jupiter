package org.jupiter.rpc.provider.limiter;

import org.jupiter.rpc.Request;

/**
 * jupiter
 * org.jupiter.rpc.provider.limiter
 *
 * @author jiachun.fjc
 */
public class DefaultTPSLimiter implements TPSLimiter {

    @Override
    public TPSResult process(Request request) {
        // TODO 以APP为最小粒度限流? 还是以方法为最小粒度? 我再仔细想想
        return new TPSResult(true, null);
    }
}
