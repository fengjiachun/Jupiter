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
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public final class SocketChannelProvider<T extends Channel> implements ChannelFactory<T> {

    public static final ChannelFactory<ServerChannel> JAVA_NIO_ACCEPTOR =
            new SocketChannelProvider<>(SocketType.JAVA_NIO, ChannelType.ACCEPTOR);
    public static final ChannelFactory<ServerChannel> NATIVE_EPOLL_ACCEPTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_EPOLL, ChannelType.ACCEPTOR);
    public static final ChannelFactory<ServerChannel> NATIVE_KQUEUE_ACCEPTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_KQUEUE, ChannelType.ACCEPTOR);
    public static final ChannelFactory<ServerChannel> NATIVE_EPOLL_DOMAIN_ACCEPTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_EPOLL_DOMAIN, ChannelType.ACCEPTOR);
    public static final ChannelFactory<ServerChannel> NATIVE_KQUEUE_DOMAIN_ACCEPTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_KQUEUE_DOMAIN, ChannelType.ACCEPTOR);

    public static final ChannelFactory<Channel> JAVA_NIO_CONNECTOR =
            new SocketChannelProvider<>(SocketType.JAVA_NIO, ChannelType.CONNECTOR);
    public static final ChannelFactory<Channel> NATIVE_EPOLL_CONNECTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_EPOLL, ChannelType.CONNECTOR);
    public static final ChannelFactory<Channel> NATIVE_KQUEUE_CONNECTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_KQUEUE, ChannelType.CONNECTOR);
    public static final ChannelFactory<Channel> NATIVE_EPOLL_DOMAIN_CONNECTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_EPOLL_DOMAIN, ChannelType.CONNECTOR);
    public static final ChannelFactory<Channel> NATIVE_KQUEUE_DOMAIN_CONNECTOR =
            new SocketChannelProvider<>(SocketType.NATIVE_KQUEUE_DOMAIN, ChannelType.CONNECTOR);

    public SocketChannelProvider(SocketType socketType, ChannelType channelType) {
        this.socketType = socketType;
        this.channelType = channelType;
    }

    private final SocketType socketType;
    private final ChannelType channelType;

    @SuppressWarnings("unchecked")
    @Override
    public T newChannel() {
        switch (channelType) {
            case ACCEPTOR:
                switch (socketType) {
                    case JAVA_NIO:
                        return (T) new NioServerSocketChannel();
                    case NATIVE_EPOLL:
                        return (T) new EpollServerSocketChannel();
                    case NATIVE_KQUEUE:
                        return (T) new KQueueServerSocketChannel();
                    case NATIVE_EPOLL_DOMAIN:
                        return (T) new EpollServerDomainSocketChannel();
                    case NATIVE_KQUEUE_DOMAIN:
                        return (T) new KQueueServerDomainSocketChannel();
                    default:
                        throw new IllegalStateException("Invalid socket type: " + socketType);
                }
            case CONNECTOR:
                switch (socketType) {
                    case JAVA_NIO:
                        return (T) new NioSocketChannel();
                    case NATIVE_EPOLL:
                        return (T) new EpollSocketChannel();
                    case NATIVE_KQUEUE:
                        return (T) new KQueueSocketChannel();
                    case NATIVE_EPOLL_DOMAIN:
                        return (T) new EpollDomainSocketChannel();
                    case NATIVE_KQUEUE_DOMAIN:
                        return (T) new KQueueDomainSocketChannel();
                    default:
                        throw new IllegalStateException("Invalid socket type: " + socketType);
                }
            default:
                throw new IllegalStateException("Invalid channel type: " + channelType);
        }
    }

    public enum SocketType {
        JAVA_NIO,
        NATIVE_EPOLL,           // for linux
        NATIVE_KQUEUE,          // for bsd systems
        NATIVE_EPOLL_DOMAIN,    // unix domain socket for linux
        NATIVE_KQUEUE_DOMAIN    // unix domain socket for bsd systems
    }

    public enum ChannelType {
        ACCEPTOR,
        CONNECTOR
    }
}
