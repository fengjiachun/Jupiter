package org.jupiter.example.round;

import org.jupiter.example.ServiceTest;
import org.jupiter.example.ServiceTest2;
import org.jupiter.rpc.InvokeMode;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.rpc.consumer.future.JFuture;
import org.jupiter.rpc.consumer.invoker.FutureInvoker;
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
        NettyConnector connector = new JNettyTcpConnector();
        // 连接ConfigServer
        connector.connectToConfigServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionManager manager1 = connector.manageConnections(ServiceTest.class);
        JConnector.ConnectionManager manager2 = connector.manageConnections(ServiceTest2.class);
        // 等待连接可用
        if (!manager1.waitForAvailable(3000) && !manager2.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        ServiceTest service1 = ProxyFactory.factory(ServiceTest.class)
                .connector(connector)
                .invokeMode(InvokeMode.FUTURE)
                .newProxyInstance();
        ServiceTest2 service2 = ProxyFactory.factory(ServiceTest2.class)
                .connector(connector)
                .invokeMode(InvokeMode.FUTURE)
                .newProxyInstance();

        try {
            ServiceTest.ResultClass result1 = service1.sayHello();
            System.out.println(result1);
            JFuture future1 = FutureInvoker.future();

            String result2 = service2.sayHelloString();
            System.out.println(result2);
            JFuture future2 = FutureInvoker.future();

            System.out.println("future.get: " + future1.get() + " | " + future2.get());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
