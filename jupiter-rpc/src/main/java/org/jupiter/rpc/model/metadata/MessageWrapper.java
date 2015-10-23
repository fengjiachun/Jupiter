package org.jupiter.rpc.model.metadata;

import org.jupiter.rpc.Directory;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Request数据的包装对象
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class MessageWrapper extends Directory implements Serializable {

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

    @Override
    public String getGroup() {
        return metadata.getGroup();
    }

    public void setGroup(String group) {
        metadata.setGroup(group);
    }

    @Override
    public String getVersion() {
        return metadata.getVersion();
    }

    public void setVersion(String version) {
        metadata.setVersion(version);
    }

    @Override
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
