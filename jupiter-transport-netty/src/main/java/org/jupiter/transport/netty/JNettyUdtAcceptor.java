package org.jupiter.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import org.jupiter.rpc.provider.processor.DefaultProviderProcessor;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JOption;
import org.jupiter.transport.netty.handler.IdleStateChecker;
import org.jupiter.transport.netty.handler.ProtocolDecoder;
import org.jupiter.transport.netty.handler.ProtocolEncoder;
import org.jupiter.transport.netty.handler.acceptor.AcceptorHandler;
import org.jupiter.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;

import java.net.SocketAddress;

import static org.jupiter.common.util.JConstants.READER_IDLE_TIME_SECONDS;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class JNettyUdtAcceptor extends NettyUdtAcceptor {

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final AcceptorHandler handler = new AcceptorHandler(new DefaultProviderProcessor(this));
    private final ProtocolEncoder encoder = new ProtocolEncoder();

    public JNettyUdtAcceptor(int port) {
        super(port);
    }

    public JNettyUdtAcceptor(SocketAddress address) {
        super(address);
    }

    public JNettyUdtAcceptor(int port, int nWorks) {
        super(port, nWorks);
    }

    public JNettyUdtAcceptor(SocketAddress address, int nWorks) {
        super(address, nWorks);
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 1024);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress address) {
        ServerBootstrap boot = bootstrap();

        boot.channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                .childHandler(new ChannelInitializer<UdtChannel>() {

                    @Override
                    protected void initChannel(UdtChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new IdleStateChecker(timer, READER_IDLE_TIME_SECONDS, 0, 0),
                                idleStateTrigger,
                                new ProtocolDecoder(),
                                encoder,
                                handler);
                    }
                });

        setOptions();

        return boot.bind(address);
    }
}
