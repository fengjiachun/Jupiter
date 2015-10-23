package org.jupiter.registry;

import java.util.Collection;

import static org.jupiter.registry.RegisterMeta.*;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryService {

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
     * 订阅provider下线通知
     */
    void subscribe(Address address, OfflineListener listener);

    /**
     * 本地查找服务
     */
    Collection<RegisterMeta> lookup(ServiceMeta serviceMeta);
}
