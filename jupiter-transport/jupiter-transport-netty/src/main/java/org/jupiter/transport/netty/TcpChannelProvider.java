/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.transport.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public final class TcpChannelProvider<T extends Channel> implements ChannelFactory<T> {

    public static final ChannelFactory<ServerChannel> NIO_ACCEPTOR = new TcpChannelProvider<>(TypeIO.NIO, KindChannel.ACCEPTOR);
    public static final ChannelFactory<ServerChannel> NATIVE_ACCEPTOR = new TcpChannelProvider<>(TypeIO.NATIVE, KindChannel.ACCEPTOR);

    public static final ChannelFactory<Channel> NIO_CONNECTOR = new TcpChannelProvider<>(TypeIO.NIO, KindChannel.CONNECTOR);
    public static final ChannelFactory<Channel> NATIVE_CONNECTOR = new TcpChannelProvider<>(TypeIO.NATIVE, KindChannel.CONNECTOR);

    public TcpChannelProvider(TypeIO typeIO, KindChannel kindChannel) {
        this.typeIO = typeIO;
        this.kindChannel = kindChannel;
    }

    private final TypeIO typeIO;
    private final KindChannel kindChannel;

    @SuppressWarnings("unchecked")
    @Override
    public T newChannel() {
        switch (kindChannel) {
            case ACCEPTOR:
                switch (typeIO) {
                    case NIO:
                        return (T) new NioServerSocketChannel();
                    case NATIVE:
                        return (T) new EpollServerSocketChannel();
                    default:
                        throw new IllegalStateException("invalid type IO: " + typeIO);
                }
            case CONNECTOR:
                switch (typeIO) {
                    case NIO:
                        return (T) new NioSocketChannel();
                    case NATIVE:
                        return (T) new EpollSocketChannel();
                    default:
                        throw new IllegalStateException("invalid type IO: " + typeIO);
                }
            default:
                throw new IllegalStateException("invalid kind channel: " + kindChannel);
        }
    }

    public enum TypeIO {
        NIO,
        NATIVE
    }

    public enum KindChannel {
        ACCEPTOR,
        CONNECTOR
    }
}
