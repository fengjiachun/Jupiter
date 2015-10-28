package org.jupiter.registry;

import java.util.Collection;

import static org.jupiter.registry.RegisterMeta.*;

/**
 * Registry service.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryService {

    /**
     * 初始化并与ConfigServer建立连接
     */
    void init(Object... args);

    /**
     * 注册服务
     */
    void register(RegisterMeta meta);

    /**
     * 取消注册服务
     */
    void unregister(RegisterMeta meta);

    /**
     * 订阅服务
     */
    void subscribe(ServiceMeta serviceMeta, NotifyListener listener);

    /**
     * provider下线通知
     */
    void offlineListening(Address address, OfflineListener listener);

    /**
     * 本地查找服务
     */
    Collection<RegisterMeta> lookup(ServiceMeta serviceMeta);
}
