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
     * 获取所有Publisher的地址列表
     */
    List<String> listPublisherHosts();

    /**
     * 获取所有Subscriber的地址列表
     */
    List<String> listSubscriberAddresses();
}
