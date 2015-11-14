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

package org.jupiter.rpc.model.metadata;

import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.provider.limiter.TpsLimiter;

import java.io.Serializable;
import java.util.concurrent.Executor;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * Wrapper provider object and service metadata.
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ServiceWrapper implements Serializable {

    private static final long serialVersionUID = 4525569114144366337L;

    private final ServiceMetadata metadata;
    private final Object serviceProvider;

    private volatile Executor executor;
    private volatile TpsLimiter<JRequest> tpsLimiter;

    public ServiceWrapper(String group, String version, String name, Object serviceProvider) {
        checkNotNull(group, "group");
        checkNotNull(version, "version");
        checkNotNull(name, "serviceProviderName");
        checkNotNull(serviceProvider, "serviceProvider");

        metadata = new ServiceMetadata(group, version, name);
        this.serviceProvider = serviceProvider;
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public Object getServiceProvider() {
        return serviceProvider;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public TpsLimiter<JRequest> getTpsLimiter() {
        return tpsLimiter;
    }

    public void setTpsLimiter(TpsLimiter<JRequest> tpsLimiter) {
        this.tpsLimiter = tpsLimiter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceWrapper wrapper = (ServiceWrapper) o;

        return metadata.equals(wrapper.metadata);
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    @Override
    public String toString() {
        return "ServiceWrapper{" +
                "metadata=" + metadata +
                ", serviceProvider=" + serviceProvider +
                ", executor=" + executor +
                ", tpsLimiter=" + tpsLimiter +
                '}';
    }
}
