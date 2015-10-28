package org.jupiter.rpc.provider.limiter;

import org.jupiter.rpc.Request;

/**
 * TPS 服务限流, 默认的实现为 {@link DefaultTPSLimiter}, 基于SPI可扩展.
 *
 * jupiter
 * org.jupiter.rpc.provider.limiter
 *
 * @author jiachun.fjc
 */
public interface TPSLimiter {

    TPSResult process(Request request);
}
