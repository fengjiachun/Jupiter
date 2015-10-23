package org.jupiter.example.broadcast;

import org.jupiter.example.ServiceTestImpl;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.NettyAcceptor;
import org.jupiter.transport.netty.JNettyTcpAcceptor;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * jupiter
 * org.jupiter.example.broadcast
 *
 * @author jiachun.fjc
 */
public class HelloJupiterServer {

    public static void main(String[] args) {
        NettyAcceptor acceptor1 = new JNettyTcpAcceptor(18090);
        NettyAcceptor acceptor2 = new JNettyTcpAcceptor(18091);

        NettyAcceptor[] servers = { acceptor1, acceptor2 };
        final CountDownLatch latch = new CountDownLatch(servers.length);
        for (final NettyAcceptor acceptor : servers) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ServiceWrapper provider = acceptor.serviceRegistry()
                                .provider(new ServiceTestImpl())
                                .register();

                        int port = ((InetSocketAddress) acceptor.localAddress()).getPort();
                        acceptor.publish(provider, port);
                        acceptor.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
