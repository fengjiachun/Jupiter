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

/**
 * Registry server register meta information.
 *
 * 注意: 不要轻易修改成员变量, 否则将影响hashCode和equals, RegisterMeta需要经常放入List, Map等容器中.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class RegisterMeta {

    // 地址
    private Address address = new Address();
    // metadata
    private ServiceMeta serviceMeta = new ServiceMeta();
    // 权重 hashCode() 与 equals() 不把weight计算在内
    private volatile int weight;
    // 建议连接数, jupiter客户端会根据connCount的值去建立对应数量的连接, hashCode() 与 equals() 不把connCount计算在内
    private volatile int connCount;

    public String getHost() {
        return address.getHost();
    }

    public void setHost(String host) {
        address.setHost(host);
    }

    public int getPort() {
        return address.getPort();
    }

    public void setPort(int port) {
        address.setPort(port);
    }

    public String getGroup() {
        return serviceMeta.getGroup();
    }

    public void setGroup(String group) {
        serviceMeta.setGroup(group);
    }

    public String getServiceProviderName() {
        return serviceMeta.getServiceProviderName();
    }

    public void setServiceProviderName(String serviceProviderName) {
        serviceMeta.setServiceProviderName(serviceProviderName);
    }

    public String getVersion() {
        return serviceMeta.getVersion();
    }

    public void setVersion(String version) {
        serviceMeta.setVersion(version);
    }

    public Address getAddress() {
        return address;
    }

    public ServiceMeta getServiceMeta() {
        return serviceMeta;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getConnCount() {
        return connCount;
    }

    public void setConnCount(int connCount) {
        this.connCount = connCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegisterMeta that = (RegisterMeta) o;

        return !(address != null ? !address.equals(that.address) : that.address != null)
                && !(serviceMeta != null ? !serviceMeta.equals(that.serviceMeta) : that.serviceMeta != null);
    }

    @Override
    public int hashCode() {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (serviceMeta != null ? serviceMeta.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RegisterMeta{" +
                "address=" + address +
                ", serviceMeta=" + serviceMeta +
                ", weight=" + weight +
                ", connCount=" + connCount +
                '}';
    }

    /**
     * 不要轻易修改成员变量, 否则将影响hashCode和equals, Address需要经常放入List, Map等容器中.
     */
    public static class Address {
        // 地址
        private String host;
        // 端口
        private int port;

        public Address() {}

        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Address address = (Address) o;

            return port == address.port && !(host != null ? !host.equals(address.host) : address.host != null);
        }

        @Override
        public int hashCode() {
            int result = host != null ? host.hashCode() : 0;
            result = 31 * result + port;
            return result;
        }

        @Override
        public String toString() {
            return "Address{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    /**
     * 不要轻易修改成员变量, 否则将影响hashCode和equals, ServiceMeta需要经常放入List, Map等容器中.
     */
    public static class ServiceMeta {
        // 组别
        private String group;
        // 服务名
        private String serviceProviderName;
        // 版本信息
        private String version;

        public ServiceMeta() {}

        public ServiceMeta(String group, String serviceProviderName, String version) {
            this.group = group;
            this.serviceProviderName = serviceProviderName;
            this.version = version;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getServiceProviderName() {
            return serviceProviderName;
        }

        public void setServiceProviderName(String serviceProviderName) {
            this.serviceProviderName = serviceProviderName;
        }

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

            ServiceMeta that = (ServiceMeta) o;

            return !(group != null ? !group.equals(that.group) : that.group != null)
                    && !(serviceProviderName != null ? !serviceProviderName.equals(that.serviceProviderName) : that.serviceProviderName != null)
                    && !(version != null ? !version.equals(that.version) : that.version != null);
        }

        @Override
        public int hashCode() {
            int result = group != null ? group.hashCode() : 0;
            result = 31 * result + (serviceProviderName != null ? serviceProviderName.hashCode() : 0);
            result = 31 * result + (version != null ? version.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ServiceMeta{" +
                    "group='" + group + '\'' +
                    ", serviceProviderName='" + serviceProviderName + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }
    }
}
