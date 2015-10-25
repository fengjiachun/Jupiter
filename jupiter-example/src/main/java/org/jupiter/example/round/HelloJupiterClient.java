package org.jupiter.example.round;

import org.jupiter.example.ServiceTest;
import org.jupiter.rpc.Directory;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterClient {

    public static void main(String[] args) {
        Directory directory = new ServiceMetadata("test", "1.0.0.daily", "ServiceTest");

        NettyConnector connector = new JNettyTcpConnector();
        // 连接ConfigServer
        connector.initRegistryService("127.0.0.1", 20001);
        // 自动管理可用连接
        JConnector.ConnectionManagement management = connector.manageConnections(directory);
        // 等待连接可用
        connector.waitForAvailable(management, 3000);

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
