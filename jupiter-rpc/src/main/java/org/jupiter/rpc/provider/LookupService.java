package org.jupiter.rpc.provider;

import org.jupiter.rpc.Directory;
import org.jupiter.rpc.model.metadata.ServiceWrapper;

/**
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
