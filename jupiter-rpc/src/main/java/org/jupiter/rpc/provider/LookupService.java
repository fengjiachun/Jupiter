package org.jupiter.rpc.provider;

import org.jupiter.rpc.Directory;
import org.jupiter.rpc.model.metadata.ServiceWrapper;

/**
 * Lookup designated service.
 *
 * jupiter
 * org.jupiter.rpc.provider
 *
 * @author jiachun.fjc
 */
public interface LookupService {

    /**
     * 查找服务
     */
    ServiceWrapper lookupService(Directory directory);
}
