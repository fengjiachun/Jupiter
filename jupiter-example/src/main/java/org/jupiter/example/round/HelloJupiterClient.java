package org.jupiter.example.round;

import org.jupiter.example.ServiceTest;
import org.jupiter.registry.NotifyListener;
import org.jupiter.registry.OfflineListener;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.rpc.Directory;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.channel.JChannelGroup;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.JConnectionManager;
import org.jupiter.transport.netty.NettyConnector;
import org.jupiter.transport.netty.JNettyTcpConnector;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterClient {

    public static void main(String[] args) {
        final NettyConnector connector = new JNettyTcpConnector();
        connector.initRegistryService("127.0.0.1", 20001);

        final Directory directory = new ServiceMetadata("test", "1.0.0.daily", "ServiceTest");

        final CountDownLatch latch = new CountDownLatch(1);
        connector.subscribe(directory, new NotifyListener() {

            @Override
            public void notify(List<RegisterMeta> registerMetaList) {
                for (RegisterMeta meta : registerMetaList) {
                    UnresolvedAddress address = new UnresolvedAddress(meta.getHost(), meta.getPort());

                    JChannelGroup group = connector.group(address);
                    if (group.isEmpty()) {
                        JConnectionManager.manage(connector.connect(address));
                        connector.subscribe(address, new OfflineListener() {

                            @Override
                            public void offline(RegisterMeta.Address address) {
                                JConnectionManager.cancelReconnect(new UnresolvedAddress(address.getHost(), address.getPort()));
                            }
                        });
                    }
                    connector.addGroup(directory, group);
                    System.out.println(meta);
                }
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ServiceTest service = ProxyFactory
                .create()
                .connector(connector)
                .interfaceClass(ServiceTest.class)
                .newProxyInstance();

        try {
            ServiceTest.ResultClass result = service.sayHello();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
