package org.jupiter.registry;

import static org.jupiter.registry.RegisterMeta.*;

/**
 * 通知provider下线
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface OfflineListener {

    void offline(Address address);
}
