package org.jupiter.example.round;

import org.jupiter.example.ServiceTest;
import org.jupiter.rpc.Directory;
import org.jupiter.rpc.InvokeMode;
import org.jupiter.rpc.JListener;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.transport.JConnector;
import org.jupiter.transport.exception.ConnectFailedException;
import org.jupiter.transport.netty.JNettyTcpConnector;
import org.jupiter.transport.netty.NettyConnector;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterCallbackClient {

    public static void main(String[] args) {
        Directory directory = new ServiceMetadata("test", "1.0.0.daily", "ServiceTest");

        NettyConnector connector = new JNettyTcpConnector();
        // 连接ConfigServer
        connector.connectToConfigServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionManager manager = connector.manageConnections(directory);
        // 等待连接可用
        if (!manager.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .connector(connector)
                .invokeMode(InvokeMode.CALLBACK)
                .listener(new JListener() {
                    @Override
                    public void complete(JRequest request, JResult result) throws Exception {
                        System.out.println("complete: " + result);
                    }

                    @Override
                    public void failure(JRequest request, Throwable cause) {
                        System.out.println("failure: " + cause);
                    }
                })
                .newProxyInstance();

        try {
            ServiceTest.ResultClass result = service.sayHello();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
