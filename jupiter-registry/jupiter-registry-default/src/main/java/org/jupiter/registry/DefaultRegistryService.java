/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.registry;

import org.jupiter.common.util.Maps;
import org.jupiter.common.util.SpiMetadata;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.transport.JConnection;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.UnresolvedSocketAddress;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * Default registry service.
 *
 * jupiter
 * org.jupiter.registry.jupiter
 *
 * @author jiachun.fjc
 */
@SpiMetadata(name = "default")
public class DefaultRegistryService extends AbstractRegistryService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultRegistryService.class);

    private final ConcurrentMap<UnresolvedAddress, DefaultRegistry> clients = Maps.newConcurrentMap();

    @Override
    protected void doSubscribe(RegisterMeta.ServiceMeta serviceMeta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Subscribe: {}.", serviceMeta);

        for (DefaultRegistry c : allClients) {
            c.doSubscribe(serviceMeta);
        }
    }

    @Override
    protected void doRegister(RegisterMeta meta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Register: {}.", meta);

        for (DefaultRegistry c : allClients) {
            c.doRegister(meta);
        }
        getRegisterMetaMap().put(meta, RegisterState.DONE);
    }

    @SuppressWarnings("all")
    @Override
    protected void doUnregister(RegisterMeta meta) {
        Collection<DefaultRegistry> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Unregister: {}.", meta);

        for (DefaultRegistry c : allClients) {
            c.doUnregister(meta);
        }
    }

    @Override
    protected void doCheckRegisterNodeStatus() {
        // the default registry service does nothing
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        checkNotNull(connectString, "connectString");

        String[] array = Strings.split(connectString, ',');
        for (String s : array) {
            String[] addressStr = Strings.split(s, ':');
            String host = addressStr[0];
            int port = Integer.parseInt(addressStr[1]);
            UnresolvedAddress address = new UnresolvedSocketAddress(host, port);
            DefaultRegistry client = clients.get(address);
            if (client == null) {
                DefaultRegistry newClient = new DefaultRegistry(this);
                client = clients.putIfAbsent(address, newClient);
                if (client == null) {
                    client = newClient;
                    JConnection connection = client.connect(address);
                    client.connectionManager().manage(connection);
                } else {
                    newClient.shutdownGracefully();
                }
            }
        }
    }

    @Override
    public void destroy() {
        for (DefaultRegistry c : clients.values()) {
            c.shutdownGracefully();
        }
    }
}