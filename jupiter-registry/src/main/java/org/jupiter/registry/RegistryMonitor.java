package org.jupiter.registry;

import java.util.List;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public interface RegistryMonitor {

    /**
     * 获取所有Provider的地址列表
     */
    List<String> getAllProviderHost();

    /**
     * 获取所有Consumer的地址列表
     */
    List<String> getAllConsumerHost();
}
