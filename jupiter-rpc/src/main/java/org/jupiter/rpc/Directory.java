package org.jupiter.rpc;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class Directory {

    /**
     * 服务所属组别
     */
    public abstract String getGroup();

    /**
     * 服务版本
     */
    public abstract String getVersion();

    /**
     * 服务名称
     */
    public abstract String getServiceProviderName();

    public String directory() {
        return getGroup() + '-' + getVersion() + '-' + getServiceProviderName();
    }
}
