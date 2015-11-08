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
import java.util.Arrays;

/**
 * Request data wrapper.
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class MessageWrapper implements Serializable {

    private static final long serialVersionUID = 1009813828866652852L;

    // 应用名称
    private String appName;
    private ServiceMetadata metadata = new ServiceMetadata();
    // 方法名称
    private String methodName;
    // 方法参数类型
    private Class<?>[] parameterTypes;
    // 方法参数
    private Object[] args;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public String getGroup() {
        return metadata.getGroup();
    }

    public void setGroup(String group) {
        metadata.setGroup(group);
    }

    public String getVersion() {
        return metadata.getVersion();
    }

    public void setVersion(String version) {
        metadata.setVersion(version);
    }

    public String getServiceProviderName() {
        return metadata.getServiceProviderName();
    }

    public void setServiceProviderName(String serviceProviderName) {
        metadata.setServiceProviderName(serviceProviderName);
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "MessageWrapper{" +
                "appName='" + appName + '\'' +
                ", metadata=" + metadata +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", args=" + Arrays.toString(args) +
                '}';
    }
}
