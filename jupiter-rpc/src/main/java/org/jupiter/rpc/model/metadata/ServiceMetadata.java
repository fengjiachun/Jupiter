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

import java.io.Serializable;

import org.jupiter.transport.Directory;

import static org.jupiter.common.util.Requires.requireNotNull;

/**
 * Service provider's metadata.
 *
 * 服务的元数据.
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ServiceMetadata extends Directory implements Serializable {

    private static final long serialVersionUID = -8908295634641380163L;

    private String group;               // 服务组别
    private String serviceProviderName; // 服务名称
    private String version;             // 服务版本号

    public ServiceMetadata() {}

    public ServiceMetadata(String group, String serviceProviderName, String version) {
        this.group = requireNotNull(group, "group");
        this.serviceProviderName = requireNotNull(serviceProviderName, "serviceProviderName");
        this.version = requireNotNull(version, "version");
    }

    @Override
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String getServiceProviderName() {
        return serviceProviderName;
    }

    public void setServiceProviderName(String serviceProviderName) {
        this.serviceProviderName = serviceProviderName;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceMetadata metadata = (ServiceMetadata) o;

        return group.equals(metadata.group)
                && serviceProviderName.equals(metadata.serviceProviderName)
                && version.equals(metadata.version);
    }

    @Override
    public int hashCode() {
        int result = group.hashCode();
        result = 31 * result + serviceProviderName.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceMetadata{" +
                "group='" + group + '\'' +
                ", serviceProviderName='" + serviceProviderName + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
