package org.jupiter.rpc.model.metadata;

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
}
