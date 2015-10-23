package org.jupiter.rpc.aop;

import org.jupiter.rpc.Request;

/**
 * jupiter
 * org.jupiter.rpc.aop
 *
 * @author jiachun.fjc
 */
public interface ConsumerHook {

    /**
     * Request数据被发送到网络前被触发
     */
    void before(Request request);

    /**
     * RPC返回结果时被触发
     */
    void after(Request request);
}
