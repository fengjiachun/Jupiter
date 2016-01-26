package org.jupiter.example.round;

import org.jupiter.example.ServiceTest;
import org.jupiter.rpc.Directory;
import org.jupiter.rpc.InvokeMode;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.future.JFuture;
import org.jupiter.rpc.consumer.invoker.FutureInvoker;
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
public class HelloJupiterFutureClient {

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
                .invokeMode(InvokeMode.FUTURE)
                .newProxyInstance();

        try {
            ServiceTest.ResultClass result = service.sayHello();
            System.out.println(result);

            JFuture future = FutureInvoker.future();
            System.out.println("future.get: " + future.get());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
