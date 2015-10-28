package org.jupiter.registry;

import java.util.List;

/**
 * Registry monitor.
 *
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

    /**
     * 根据指定服务返回改服务的全部提供者的地址
     */
    List<String> listAddressesByService(String group, String version, String serviceProviderName);

    /**
     * 根据指定地址返回该地址对应的节点提供的所有服务
     */
    List<String> listServicesByAddress(String host, int port);
}
