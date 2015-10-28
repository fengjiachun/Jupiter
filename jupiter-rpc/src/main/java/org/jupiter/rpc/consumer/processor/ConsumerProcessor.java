package org.jupiter.rpc.consumer.processor;

import org.jupiter.rpc.Response;
import org.jupiter.rpc.channel.JChannel;

/**
 * Consumer's processor.
 *
 * jupiter
 * org.jupiter.rpc.consumer.processor
 *
 * @author jiachun.fjc
 */
public interface ConsumerProcessor {

    void handleResponse(JChannel ch, Response response) throws Exception;
}
