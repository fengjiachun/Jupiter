package org.jupiter.example.broadcast;

import org.jupiter.example.ServiceTest;
import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.netty.NettyConnector;
import org.jupiter.transport.netty.JNettyTcpConnector;

/**
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class HelloJupiterClient {

    public static void main(String[] args) {
        NettyConnector connector = new JNettyTcpConnector();
        UnresolvedAddress address1 = new UnresolvedAddress("127.0.0.1", 18090);
        UnresolvedAddress address2 = new UnresolvedAddress("127.0.0.1", 18091);
        UnresolvedAddress address3 = new UnresolvedAddress("127.0.0.1", 18090);
        connector.connect(address1);
        connector.connect(address2);
        connector.connect(address3);

        ServiceTest service = ProxyFactory
                .create()
                .connector(connector)
                .dispatchMode(DispatchMode.BROADCAST)
                .asyncMode(AsyncMode.ASYNC_CALLBACK)
                .addProviderAddress(address1, address2, address2)
                .interfaceClass(ServiceTest.class)
                .listener(new JListener() {

                    @Override
                    public void complete(Request request, Object result) throws Exception {
                        System.out.println("complete=" + result);
                    }

                    @Override
                    public void failure(Request request, Throwable cause) {
                        System.out.println("failure=" + cause);
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
