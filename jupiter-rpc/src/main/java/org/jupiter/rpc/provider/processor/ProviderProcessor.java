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

    void handleRequest(JChannel ch, Request request) throws Exception;

    void handleException(JChannel ch, Request request, Throwable cause);
}
