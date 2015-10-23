package org.jupiter.rpc.provider.processor;

import org.jupiter.rpc.Request;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.provider.LookupService;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public interface ProviderProcessor extends LookupService {

    /**
     * 处理正常请求
     */
    void handleRequest(JChannel ch, Request request) throws Exception;

    /**
     * 处理异常
     */
    void handleException(JChannel ch, Request request, Throwable cause);
}
