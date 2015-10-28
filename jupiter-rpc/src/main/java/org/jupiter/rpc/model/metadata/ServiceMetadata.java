package org.jupiter.rpc.model.metadata;

import org.jupiter.rpc.Directory;

import java.io.Serializable;

/**
 * Service provider metadata
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ServiceMetadata extends Directory implements Serializable {

    private static final long serialVersionUID = -8908295634641380163L;

    private String group;
    private String version;
    private String serviceProviderName;

    public ServiceMetadata() {}

    public ServiceMetadata(String group, String version, String serviceProviderName) {
        this.group = group;
        this.version = version;
        this.serviceProviderName = serviceProviderName;
    }

    @Override
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getServiceProviderName() {
        return serviceProviderName;
    }

    public void setServiceProviderName(String serviceProviderName) {
        this.serviceProviderName = serviceProviderName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceMetadata metadata = (ServiceMetadata) o;

        return group.equals(metadata.group)
                && version.equals(metadata.version)
                && serviceProviderName.equals(metadata.serviceProviderName);
    }

    @Override
    public int hashCode() {
        int result = group.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + serviceProviderName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceMetadata{" +
                "group='" + group + '\'' +
                ", version='" + version + '\'' +
                ", serviceProviderName='" + serviceProviderName + '\'' +
                '}';
    }
}
