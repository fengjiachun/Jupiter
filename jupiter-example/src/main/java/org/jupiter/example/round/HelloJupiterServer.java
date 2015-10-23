package org.jupiter.example.round;

import org.jupiter.example.ServiceTestImpl;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.NettyAcceptor;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterServer {

    public static void main(String[] args) {
        NettyAcceptor server = new JNettyTcpAcceptor(18090);
        try {
            ServiceWrapper provider = server.serviceRegistry()
                    .provider(new ServiceTestImpl())
                    .register();

            server.initRegistryService("127.0.0.1", 20001);
            server.publish(provider, 18090);
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
