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

package org.jupiter.transport;

import org.jupiter.common.util.AbstractConstant;
import org.jupiter.common.util.ConstantPool;

/**
 * Jupiter transport option.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @param <T> the type of the value which is valid for the {@link JOption}
 *
 * @author jiachun.fjc
 */
@SuppressWarnings("unchecked")
public final class JOption<T> extends AbstractConstant<JOption<T>> {

    private static final ConstantPool<JOption<Object>> pool = new ConstantPool<JOption<Object>>() {

        @Override
        protected JOption<Object> newConstant(int id, String name) {
            return new JOption<>(id, name);
        }
    };

    /**
     * Returns the {@link JOption} of the specified name.
     */
    public static <T> JOption<T> valueOf(String name) {
        return (JOption<T>) pool.valueOf(name);
    }

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */
    public static <T> JOption<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (JOption<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }

    /**
     * Creates a new {@link JOption} for the given {@param name} or fail with an
     * {@link IllegalArgumentException} if a {@link JOption} for the given {@param name} exists.
     */
    public static <T> JOption<T> newInstance(String name) {
        return (JOption<T>) pool.newInstance(name);
    }

    /**
     * Returns {@code true} if a {@link JOption} exists for the given {@code name}.
     */
    public static boolean exists(String name) {
        return pool.exists(name);
    }

    /**
     * 对此连接禁用Nagle算法.
     */
    public static final JOption<Boolean> TCP_NODELAY = valueOf("TCP_NODELAY");

    /**
     * 为TCP套接字设置keepalive选项时, 如果在2个小时（实际值与具体实现有关）内在
     * 任意方向上都没有跨越套接字交换数据, 则 TCP 会自动将 keepalive 探头发送到对端,
     * 此探头是对端必须响应的TCP段.
     *
     * 期望的响应为以下三种之一:
     * 1. 收到期望的对端ACK响应
     *      不通知应用程序(因为一切正常), 在另一个2小时的不活动时间过后，TCP将发送另一个探头。
     * 2. 对端响应RST
     *      通知本地TCP对端已崩溃并重新启动, 套接字被关闭.
     * 3. 对端没有响
     *      套接字被关闭。
     *
     * 此选项的目的是检测对端主机是否崩溃, 仅对TCP套接字有效.
     */
    public static final JOption<Boolean> KEEP_ALIVE = valueOf("KEEP_ALIVE");

    /**
     * [TCP/IP协议详解]中描述:
     * 当TCP执行一个主动关闭, 并发回最后一个ACK ,该连接必须在TIME_WAIT状态停留的时间为2倍的MSL.
     * 这样可让TCP再次发送最后的ACK以防这个ACK丢失(另一端超时并重发最后的FIN).
     * 这种2MSL等待的另一个结果是这个TCP连接在2MSL等待期间, 定义这个连接的插口对(TCP四元组)不能再被使用.
     * 这个连接只能在2MSL结束后才能再被使用.
     *
     * 许多具体的实现中允许一个进程重新使用仍处于2MSL等待的端口(通常是设置选项SO_REUSEADDR),
     * 但TCP不能允许一个新的连接建立在相同的插口对上。
     */
    public static final JOption<Boolean> SO_REUSEADDR = valueOf("SO_REUSEADDR");

    /**
     * 设置snd_buf
     * 一般对于要建立大量连接的应用, 不建议设置这个值, 因为linux内核对snd_buf的大小是动态调整的, 内核是很聪明的.
     */
    public static final JOption<Integer> SO_SNDBUF = valueOf("SO_SNDBUF");

    /**
     * 设置rcv_buf
     * 一般对于要建立大量连接的应用, 不建议设置这个值, 因为linux内核对rcv_buf的大小是动态调整的.
     */
    public static final JOption<Integer> SO_RCVBUF = valueOf("SO_RCVBUF");

    public static final JOption<Integer> SO_LINGER = valueOf("SO_LINGER");

    /**
     * 在linux内核中TCP握手过程总共会有两个队列:
     *  1) 一个俗称半连接队列, 放着那些握手一半的连接(syn queue)
     *  2) 另一个放着那些握手成功但是还没有被应用层accept的连接的队列(accept queue)
     *
     * backlog控制着accept queue的大小, 但backlog的上限是somaxconn
     * linux 2.6.20版本之前 /proc/sys/net/ipv4/tcp_max_syn_backlog决定syn queue的大小,
     * 2.6.20版本之后syn queue的大小是经过一系列复杂的计算, 那个代码我看不懂...
     *
     * 参考linux-3.10.28代码(socket.c):
     *
     * sock = sockfd_lookup_light(fd, &err, &fput_needed);
     * if (sock) {
     *     somaxconn = sock_net(sock->sk)->core.sysctl_somaxconn;
     *     if ((unsigned int)backlog > somaxconn)
     *         backlog = somaxconn;
     *
     *     err = security_socket_listen(sock, backlog);
     *     if (!err)
     *         err = sock->ops->listen(sock, backlog);
     *     fput_light(sock->file, fput_needed);
     * }
     *
     * 以上代码可以看到backlog并不是按照应用层所设置的backlog大小, 实际上取的是backlog和somaxconn的最小值.
     * somaxconn的值定义在:
     * /proc/sys/net/core/somaxconn
     *
     * 还有一点要注意, 对于TCP连接的ESTABLISHED状态, 并不需要应用层accept,
     * 只要在accept queue里就已经变成状态ESTABLISHED, 所以在使用ss或netstat排查这方面问题不要被ESTABLISHED迷惑.
     */
    public static final JOption<Integer> SO_BACKLOG = valueOf("SO_BACKLOG");

    public static final JOption<Integer> IP_TOS = valueOf("IP_TOS");

    public static final JOption<Boolean> ALLOW_HALF_CLOSURE = valueOf("ALLOW_HALF_CLOSURE");

    /**
     * 是否使用 direct buffer.
     */
    public static final JOption<Boolean> PREFER_DIRECT = valueOf("PREFER_DIRECT");

    /**
     * Netty的选项, 是否启用pooled buf allocator.
     */
    public static final JOption<Boolean> USE_POOLED_ALLOCATOR = valueOf("USE_POOLED_ALLOCATOR");

    /**
     * Netty的选项, write高水位线.
     */
    public static final JOption<Integer> WRITE_BUFFER_HIGH_WATER_MARK = valueOf("WRITE_BUFFER_HIGH_WATER_MARK");

    /**
     * Netty的选项, write低水位线.
     */
    public static final JOption<Integer> WRITE_BUFFER_LOW_WATER_MARK = valueOf("WRITE_BUFFER_LOW_WATER_MARK");

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.
     * The default value is {@code 50}, which means the event loop will try to spend the same
     * amount of time for I/O as for non-I/O tasks.
     */
    public static final JOption<Integer> IO_RATIO = valueOf("IO_RATIO");

    public static final JOption<Integer> CONNECT_TIMEOUT_MILLIS = valueOf("CONNECT_TIMEOUT_MILLIS");

    private JOption(int id, String name) {
        super(id, name);
    }
}
