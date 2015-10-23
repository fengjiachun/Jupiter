package org.jupiter.registry;

import org.jupiter.rpc.UnresolvedAddress;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.registry.RegisterMeta.*;

/**
 * jupiter
 * org.jupiter.registry.jupiter
 *
 * @author jiachun.fjc
 */
public class DefaultRegistryService extends AbstractRegistryService {

    private volatile ConfigClient configClient;

    public void connectToConfigServer(UnresolvedAddress address) {
        configClient.connect(address);
    }

    @Override
    protected void doSubscribe(ServiceMeta serviceMeta) {
        configClient.doSubscribe(serviceMeta);
    }

    @Override
    protected void doRegister(RegisterMeta meta) {
        configClient.doRegister(meta);
    }

    @Override
    protected void doUnregister(RegisterMeta meta) {
        configClient.doUnregister(meta);
    }

    @Override
    public void init(Object... args) {
        checkArgument(args.length >= 2, "need config server host and port");
        checkArgument(args[0] instanceof String, "args[0] must be a String with host");
        checkArgument(args[1] instanceof Integer, "args[1] must be a Integer with port");

        if (configClient == null) {
            configClient = new ConfigClient(this);
        }

        UnresolvedAddress address = new UnresolvedAddress((String) args[0], (Integer) args[1]);
        connectToConfigServer(address);
    }
}
