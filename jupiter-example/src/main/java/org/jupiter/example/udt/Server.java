package org.jupiter.example.udt;

import org.jupiter.example.ServiceTestImpl;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.NettyAcceptor;
import org.jupiter.transport.netty.JNettyUdtAcceptor;

/**
 * jupiter
 * org.jupiter.example.udt
 *
 * @author jiachun.fjc
 */
public class Server {

    public static void main(String[] args) {
        NettyAcceptor server = new JNettyUdtAcceptor(18090);
        try {
            ServiceWrapper provider = server.serviceRegistry()
                    .provider(new ServiceTestImpl())
                    .register();

            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
