package org.jupiter.transport.netty;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Preconditions;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JConfigGroup;
import org.jupiter.transport.JOption;

import java.util.Collections;
import java.util.List;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class NettyConfig implements JConfig {

    private volatile int ioRatio = 100;
    private volatile boolean preferDirect = true;
    private volatile boolean usePooledAllocator = true;

    @Override
    public List<JOption<?>> getOptions() {
        return getOptions(null,
                JOption.IO_RATIO,
                JOption.PREFER_DIRECT,
                JOption.USE_POOLED_ALLOCATOR);
    }

    protected List<JOption<?>> getOptions(List<JOption<?>> result, JOption<?>... options) {
        if (result == null) {
            result = Lists.newArrayList();
        }
        Collections.addAll(result, options);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(JOption<T> option) {
        Preconditions.checkNotNull(option);

        if (option == JOption.IO_RATIO) {
            return (T) Integer.valueOf(getIoRatio());
        }
        if (option == JOption.PREFER_DIRECT) {
            return (T) Boolean.valueOf(isPreferDirect());
        }
        if (option == JOption.USE_POOLED_ALLOCATOR) {
            return (T) Boolean.valueOf(isUsePooledAllocator());
        }
        return null;
    }

    @Override
    public <T> boolean setOption(JOption<T> option, T value) {
        validate(option, value);

        if (option == JOption.IO_RATIO) {
            setIoRatio((Integer) value);
        } else if (option == JOption.PREFER_DIRECT) {
            setPreferDirect((Boolean) value);
        } else if (option == JOption.USE_POOLED_ALLOCATOR) {
            setUsePooledAllocator((Boolean) value);
        } else {
            return false;
        }
        return true;
    }

    public int getIoRatio() {
        return ioRatio;
    }

    public void setIoRatio(int ioRatio) {
        if (ioRatio < 0) {
            ioRatio = 0;
        }
        if (ioRatio > 100) {
            ioRatio = 100;
        }
        this.ioRatio = ioRatio;
    }

    public boolean isPreferDirect() {
        return preferDirect;
    }

    public void setPreferDirect(boolean preferDirect) {
        this.preferDirect = preferDirect;
    }

    public boolean isUsePooledAllocator() {
        return usePooledAllocator;
    }

    public void setUsePooledAllocator(boolean usePooledAllocator) {
        this.usePooledAllocator = usePooledAllocator;
    }

    protected <T> void validate(JOption<T> option, T value) {
        Preconditions.checkNotNull(option, "option");
        Preconditions.checkNotNull(value, "value");
    }

    /**
     * TCP netty option
     */
    public static class NettyTCPConfigGroup implements JConfigGroup {

        private ParentConfig parent = new ParentConfig();
        private ChildConfig child = new ChildConfig();

        @Override
        public ParentConfig parent() {
            return parent;
        }

        @Override
        public ChildConfig child() {
            return child;
        }

        /**
         * TCP netty parent option
         */
        public static class ParentConfig extends NettyConfig {

            private volatile int backlog = 1024;
            private volatile int rcvBuf = -1;
            private volatile boolean reuseAddress = true;

            @Override
            public List<JOption<?>> getOptions() {
                return getOptions(super.getOptions(),
                        JOption.SO_BACKLOG,
                        JOption.SO_RCVBUF,
                        JOption.SO_REUSEADDR);
            }

            protected List<JOption<?>> getOptions(List<JOption<?>> result, JOption<?>... options) {
                if (result == null) {
                    result = Lists.newArrayList();
                }
                Collections.addAll(result, options);
                return result;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getOption(JOption<T> option) {
                Preconditions.checkNotNull(option);

                if (option == JOption.SO_BACKLOG) {
                    return (T) Integer.valueOf(getBacklog());
                }
                if (option == JOption.SO_RCVBUF) {
                    return (T) Integer.valueOf(getRcvBuf());
                }
                if (option == JOption.SO_REUSEADDR) {
                    return (T) Boolean.valueOf(isReuseAddress());
                }

                return super.getOption(option);
            }

            @Override
            public <T> boolean setOption(JOption<T> option, T value) {
                validate(option, value);

                if (option == JOption.SO_BACKLOG) {
                    setIoRatio((Integer) value);
                } else if (option == JOption.SO_RCVBUF) {
                    setRcvBuf((Integer) value);
                } else if (option == JOption.SO_REUSEADDR) {
                    setReuseAddress((Boolean) value);
                } else {
                    return super.setOption(option, value);
                }

                return true;
            }

            public int getBacklog() {
                return backlog;
            }

            public void setBacklog(int backlog) {
                this.backlog = backlog;
            }

            public int getRcvBuf() {
                return rcvBuf;
            }

            public void setRcvBuf(int rcvBuf) {
                this.rcvBuf = rcvBuf;
            }

            public boolean isReuseAddress() {
                return reuseAddress;
            }

            public void setReuseAddress(boolean reuseAddress) {
                this.reuseAddress = reuseAddress;
            }
        }

        /**
         * TCP netty child option
         */
        public static class ChildConfig extends NettyConfig {
            private volatile int rcvBuf = -1;
            private volatile int sndBuf = -1;
            private volatile int linger = -1;
            private volatile int ipTos = -1;
            private volatile int connectTimeoutMillis = -1;
            private volatile boolean reuseAddress = true;
            private volatile boolean keepAlive = true;
            private volatile boolean tcpNoDelay = true;
            private volatile boolean allowHalfClosure = false;

            @Override
            public List<JOption<?>> getOptions() {
                return getOptions(super.getOptions(),
                        JOption.SO_RCVBUF,
                        JOption.SO_SNDBUF,
                        JOption.SO_LINGER,
                        JOption.SO_REUSEADDR,
                        JOption.CONNECT_TIMEOUT_MILLIS,
                        JOption.KEEP_ALIVE,
                        JOption.TCP_NODELAY,
                        JOption.IP_TOS,
                        JOption.ALLOW_HALF_CLOSURE);
            }

            protected List<JOption<?>> getOptions(List<JOption<?>> result, JOption<?>... options) {
                if (result == null) {
                    result = Lists.newArrayList();
                }
                Collections.addAll(result, options);
                return result;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getOption(JOption<T> option) {
                Preconditions.checkNotNull(option);

                if (option == JOption.SO_RCVBUF) {
                    return (T) Integer.valueOf(getRcvBuf());
                }
                if (option == JOption.SO_SNDBUF) {
                    return (T) Integer.valueOf(getSndBuf());
                }
                if (option == JOption.SO_LINGER) {
                    return (T) Integer.valueOf(getLinger());
                }
                if (option == JOption.IP_TOS) {
                    return (T) Integer.valueOf(getIpTos());
                }
                if (option == JOption.CONNECT_TIMEOUT_MILLIS) {
                    return (T) Integer.valueOf(getConnectTimeoutMillis());
                }
                if (option == JOption.SO_REUSEADDR) {
                    return (T) Boolean.valueOf(isReuseAddress());
                }
                if (option == JOption.KEEP_ALIVE) {
                    return (T) Boolean.valueOf(isKeepAlive());
                }
                if (option == JOption.TCP_NODELAY) {
                    return (T) Boolean.valueOf(isTcpNoDelay());
                }
                if (option == JOption.ALLOW_HALF_CLOSURE) {
                    return (T) Boolean.valueOf(isAllowHalfClosure());
                }

                return super.getOption(option);
            }

            @Override
            public <T> boolean setOption(JOption<T> option, T value) {
                validate(option, value);

                if (option == JOption.SO_RCVBUF) {
                    setRcvBuf((Integer) value);
                } else if (option == JOption.SO_SNDBUF) {
                    setSndBuf((Integer) value);
                } else if (option == JOption.SO_LINGER) {
                    setLinger((Integer) value);
                } else if (option == JOption.IP_TOS) {
                    setIpTos((Integer) value);
                } else if (option == JOption.CONNECT_TIMEOUT_MILLIS) {
                    setConnectTimeoutMillis((Integer) value);
                } else if (option == JOption.SO_REUSEADDR) {
                    setReuseAddress((Boolean) value);
                } else if (option == JOption.KEEP_ALIVE) {
                    setKeepAlive((Boolean) value);
                } else if (option == JOption.TCP_NODELAY) {
                    setTcpNoDelay((Boolean) value);
                } else if (option == JOption.ALLOW_HALF_CLOSURE) {
                    setAllowHalfClosure((Boolean) value);
                } else {
                    return super.setOption(option, value);
                }

                return true;
            }

            public int getRcvBuf() {
                return rcvBuf;
            }

            public void setRcvBuf(int rcvBuf) {
                this.rcvBuf = rcvBuf;
            }

            public int getSndBuf() {
                return sndBuf;
            }

            public void setSndBuf(int sndBuf) {
                this.sndBuf = sndBuf;
            }

            public int getLinger() {
                return linger;
            }

            public void setLinger(int linger) {
                this.linger = linger;
            }

            public int getIpTos() {
                return ipTos;
            }

            public void setIpTos(int ipTos) {
                this.ipTos = ipTos;
            }

            public int getConnectTimeoutMillis() {
                return connectTimeoutMillis;
            }

            public void setConnectTimeoutMillis(int connectTimeoutMillis) {
                this.connectTimeoutMillis = connectTimeoutMillis;
            }

            public boolean isReuseAddress() {
                return reuseAddress;
            }

            public void setReuseAddress(boolean reuseAddress) {
                this.reuseAddress = reuseAddress;
            }

            public boolean isKeepAlive() {
                return keepAlive;
            }

            public void setKeepAlive(boolean keepAlive) {
                this.keepAlive = keepAlive;
            }

            public boolean isTcpNoDelay() {
                return tcpNoDelay;
            }

            public void setTcpNoDelay(boolean tcpNoDelay) {
                this.tcpNoDelay = tcpNoDelay;
            }

            public boolean isAllowHalfClosure() {
                return allowHalfClosure;
            }

            public void setAllowHalfClosure(boolean allowHalfClosure) {
                this.allowHalfClosure = allowHalfClosure;
            }
        }
    }

    /**
     * UDT netty option
     */
    public static class NettyUDTConfigGroup implements JConfigGroup {

        private ParentConfig parent = new ParentConfig();
        private ChildConfig child = new ChildConfig();

        @Override
        public ParentConfig parent() {
            return parent;
        }

        @Override
        public ChildConfig child() {
            return child;
        }

        /**
         * UDT netty parent option
         */
        public static class ParentConfig extends NettyConfig {
            private volatile int backlog = 1024;

            @Override
            public List<JOption<?>> getOptions() {
                return getOptions(super.getOptions(),
                        JOption.SO_BACKLOG);
            }

            protected List<JOption<?>> getOptions(List<JOption<?>> result, JOption<?>... options) {
                if (result == null) {
                    result = Lists.newArrayList();
                }
                Collections.addAll(result, options);
                return result;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getOption(JOption<T> option) {
                Preconditions.checkNotNull(option);

                if (option == JOption.SO_BACKLOG) {
                    return (T) Integer.valueOf(getBacklog());
                }

                return super.getOption(option);
            }

            @Override
            public <T> boolean setOption(JOption<T> option, T value) {
                validate(option, value);

                if (option == JOption.SO_BACKLOG) {
                    setIoRatio((Integer) value);
                } else {
                    return super.setOption(option, value);
                }

                return true;
            }

            public int getBacklog() {
                return backlog;
            }

            public void setBacklog(int backlog) {
                this.backlog = backlog;
            }
        }

        /**
         * UDT netty child option
         */
        public static class ChildConfig extends NettyConfig {
            private volatile int rcvBuf = -1;
            private volatile int sndBuf = -1;
            private volatile int linger = -1;
            private volatile int connectTimeoutMillis = -1;
            private volatile boolean reuseAddress = true;

            @Override
            public List<JOption<?>> getOptions() {
                return getOptions(super.getOptions(),
                        JOption.SO_RCVBUF,
                        JOption.SO_SNDBUF,
                        JOption.SO_LINGER,
                        JOption.CONNECT_TIMEOUT_MILLIS,
                        JOption.SO_REUSEADDR);
            }

            protected List<JOption<?>> getOptions(List<JOption<?>> result, JOption<?>... options) {
                if (result == null) {
                    result = Lists.newArrayList();
                }
                Collections.addAll(result, options);
                return result;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getOption(JOption<T> option) {
                Preconditions.checkNotNull(option);

                if (option == JOption.SO_RCVBUF) {
                    return (T) Integer.valueOf(getRcvBuf());
                }
                if (option == JOption.SO_SNDBUF) {
                    return (T) Integer.valueOf(getSndBuf());
                }
                if (option == JOption.SO_LINGER) {
                    return (T) Integer.valueOf(getLinger());
                }
                if (option == JOption.CONNECT_TIMEOUT_MILLIS) {
                    return (T) Integer.valueOf(getConnectTimeoutMillis());
                }
                if (option == JOption.SO_REUSEADDR) {
                    return (T) Boolean.valueOf(isReuseAddress());
                }

                return super.getOption(option);
            }

            @Override
            public <T> boolean setOption(JOption<T> option, T value) {
                validate(option, value);

                if (option == JOption.SO_RCVBUF) {
                    setRcvBuf((Integer) value);
                } else if (option == JOption.SO_SNDBUF) {
                    setSndBuf((Integer) value);
                } else if (option == JOption.SO_LINGER) {
                    setLinger((Integer) value);
                } else if (option == JOption.CONNECT_TIMEOUT_MILLIS) {
                    setConnectTimeoutMillis((Integer) value);
                } else if (option == JOption.SO_REUSEADDR) {
                    setReuseAddress((Boolean) value);
                } else {
                    return super.setOption(option, value);
                }

                return true;
            }

            public int getRcvBuf() {
                return rcvBuf;
            }

            public void setRcvBuf(int rcvBuf) {
                this.rcvBuf = rcvBuf;
            }

            public int getSndBuf() {
                return sndBuf;
            }

            public void setSndBuf(int sndBuf) {
                this.sndBuf = sndBuf;
            }

            public int getLinger() {
                return linger;
            }

            public void setLinger(int linger) {
                this.linger = linger;
            }

            public int getConnectTimeoutMillis() {
                return connectTimeoutMillis;
            }

            public void setConnectTimeoutMillis(int connectTimeoutMillis) {
                this.connectTimeoutMillis = connectTimeoutMillis;
            }

            public boolean isReuseAddress() {
                return reuseAddress;
            }

            public void setReuseAddress(boolean reuseAddress) {
                this.reuseAddress = reuseAddress;
            }
        }
    }
}
