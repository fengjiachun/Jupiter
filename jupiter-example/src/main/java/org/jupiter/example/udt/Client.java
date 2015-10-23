package org.jupiter.example.udt;

import org.jupiter.example.ServiceTest;
import org.jupiter.rpc.UnresolvedAddress;
import org.jupiter.rpc.consumer.ProxyFactory;
import org.jupiter.transport.netty.NettyUdtConnector;
import org.jupiter.transport.netty.JNettyUdtConnector;

/**
 * jupiter
 * org.jupiter.example.udt
 *
 * @author jiachun.fjc
 */
public class Client {

    public static void main(String[] args) {
        NettyUdtConnector connector = new JNettyUdtConnector();
        UnresolvedAddress address = new UnresolvedAddress("127.0.0.1", 18090);
        connector.connect(address);

        ServiceTest service = ProxyFactory
                .create()
                .connector(connector)
                .addProviderAddress(address)
                .interfaceClass(ServiceTest.class)
//                .asyncMode(AsyncMode.ASYNC_CALLBACK)
//                .listener(new JListener() {
//
//                    @Override
//                    public void complete(Request request, Object result) throws Exception {
//                        System.out.println(request + ":" + result.toString());
//                    }
//
//                    @Override
//                    public void failure(Request request, Throwable cause) {
//                        System.out.println(request + ":" + cause);
//                    }
//                })
                .newProxyInstance();

        ServiceTest.ResultClass result = service.sayHello();
        System.out.println(result);
    }
}
