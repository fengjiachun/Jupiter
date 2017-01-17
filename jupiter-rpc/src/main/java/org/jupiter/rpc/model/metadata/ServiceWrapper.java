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
import org.jupiter.rpc.provider.ProviderInterceptor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.jupiter.common.util.JConstants.DEFAULT_CONNECTION_COUNT;
import static org.jupiter.common.util.JConstants.DEFAULT_WEIGHT;
import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * Wrapper provider object and service metadata.
 *
 * 服务元信息
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ServiceWrapper implements Serializable {

    private static final long serialVersionUID = 6690575889849847348L;

    private final ServiceMetadata metadata;     // 服务元信息
    private final Object serviceProvider;       // 服务对象

    // 拦截器
    private final ProviderInterceptor[] interceptors;
    // provider中所有接口的参数类型(用于根据JLS规则dispatch method)
    private final Map<String, List<Class<?>[]>> methodsParameterTypes;

    // 权重 hashCode()与equals()不把weight计算在内
    private int weight = DEFAULT_WEIGHT;
    // 建议连接数 hashCode()与equals()不把connCount计算在内
    private int connCount = DEFAULT_CONNECTION_COUNT;
    // provider私有线程池
    private Executor executor;
    // provider私有流量控制器
    private FlowController<JRequest> flowController;

    public ServiceWrapper(String group,
                          String version,
                          String name,
                          Object serviceProvider,
                          ProviderInterceptor[] interceptors,
                          Map<String, List<Class<?>[]>> methodsParameterTypes) {

        metadata = new ServiceMetadata(group, version, name);

        this.interceptors = interceptors;
        this.methodsParameterTypes = checkNotNull(methodsParameterTypes, "methodsParameterTypes");
        this.serviceProvider = checkNotNull(serviceProvider, "serviceProvider");
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public Object getServiceProvider() {
        return serviceProvider;
    }

    public ProviderInterceptor[] getInterceptors() {
        return interceptors;
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
                ", interceptors=" + Arrays.toString(interceptors) +
                ", methodsParameterTypes=" + methodsParameterTypes +
                ", weight=" + weight +
                ", connCount=" + connCount +
                ", executor=" + executor +
                ", flowController=" + flowController +
                '}';
    }
}
