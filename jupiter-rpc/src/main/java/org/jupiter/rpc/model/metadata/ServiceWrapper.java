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
import org.jupiter.rpc.flow.control.FlowController;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.DEFAULT_CONNECTION_COUNT;
import static org.jupiter.common.util.JConstants.DEFAULT_WEIGHT;
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

    private static final long serialVersionUID = 6690575889849847348L;

    private final ServiceMetadata metadata;
    private final Object serviceProvider;

    private transient Map<String, List<Class<?>[]>> methodsParameterTypes;

    // 权重 hashCode()与equals()不把weight计算在内
    private volatile int weight = DEFAULT_WEIGHT;
    // 建议连接数 hashCode()与equals()不把connCount计算在内
    private volatile int connCount = DEFAULT_CONNECTION_COUNT;
    private volatile Executor executor;
    private volatile FlowController<JRequest> flowController;

    public ServiceWrapper(String group,
                          String version,
                          String name,
                          Object serviceProvider,
                          Map<String, List<Class<?>[]>> methodsParameterTypes) {

        metadata = new ServiceMetadata(group, version, name);

        this.methodsParameterTypes = checkNotNull(methodsParameterTypes, "methodsParameterTypes");
        this.serviceProvider = checkNotNull(serviceProvider, "serviceProvider");
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public Object getServiceProvider() {
        return serviceProvider;
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

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public FlowController<JRequest> getFlowController() {
        return flowController;
    }

    public void setFlowController(FlowController<JRequest> flowController) {
        this.flowController = flowController;
    }

    public List<Class<?>[]> getMethodParameterTypes(String methodName) {
        return methodsParameterTypes.get(methodName);
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
                ", flowController=" + flowController +
                '}';
    }
}
