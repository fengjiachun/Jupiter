package org.jupiter.registry;

import java.util.List;

/**
 * 服务订阅者的监听器
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface NotifyListener {

    void notify(List<RegisterMeta> registerMetaList);
}
