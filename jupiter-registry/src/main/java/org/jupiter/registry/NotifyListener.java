package org.jupiter.registry;

import java.util.List;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface NotifyListener {

    void notify(List<RegisterMeta> registerMetaList);
}
