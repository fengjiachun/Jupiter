package org.jupiter.rpc.provider.processor;

import org.jupiter.common.util.RecycleUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.Directory;
import org.jupiter.rpc.JServer;
import org.jupiter.rpc.Request;
import org.jupiter.rpc.Response;
import org.jupiter.rpc.channel.JChannel;
import org.jupiter.rpc.model.metadata.ResultWrapper;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.serialization.SerializerHolder;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;
import static org.jupiter.rpc.Status.SERVICE_ERROR;

/**
 * jupiter
 * org.jupiter.rpc.provider.processor
 *
 * @author jiachun.fjc
 */
public abstract class AbstractProviderProcessor implements ProviderProcessor {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractProviderProcessor.class);

    private final JServer jServer;

    protected AbstractProviderProcessor(JServer jServer) {
        this.jServer = jServer;
    }

    @Override
    public void handleException(JChannel ch, Request request, Throwable cause) {
        ResultWrapper result = ResultWrapper.getInstance();
        result.setError(cause);

        Response response = new Response(request.invokeId());
        response.status(SERVICE_ERROR.value());
        try {
            response.bytes(SerializerHolder.getSerializer().writeObject(result));
        } finally {
            RecycleUtil.recycle(result);
        }

        logger.error("An exception has been caught while processing request: {}.", stackTrace(cause));

        ch.write(response);
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return jServer.lookupService(directory);
    }
}
