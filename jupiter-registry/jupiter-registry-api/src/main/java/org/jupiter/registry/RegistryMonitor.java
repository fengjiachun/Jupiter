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

import java.util.List;

/**
 * Registry monitor.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryMonitor {

    /**
     * Returns the address list of publisher.
     */
    List<String> listPublisherHosts();

    /**
     * Returns the address list of subscriber.
     */
    List<String> listSubscriberAddresses();

    /**
     * Returns to the service of all the specified service provider's address.
     */
    List<String> listAddressesByService(String group, String serviceProviderName, String version);

    /**
     * Finds the address(host, port) of the corresponding node and returns all
     * the service names it provides.
     */
    List<String> listServicesByAddress(String host, int port);
}
