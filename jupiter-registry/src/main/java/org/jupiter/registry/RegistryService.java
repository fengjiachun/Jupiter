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

    void register(RegisterMeta meta);

    void unregister(RegisterMeta meta);

    void subscribe(ServiceMeta serviceMeta, NotifyListener listener);

    /**
     * 本地查找服务
     */
    Collection<RegisterMeta> lookup(ServiceMeta serviceMeta);
}
