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

import org.jupiter.common.util.Lists;
import org.jupiter.transport.JConfig;
import org.jupiter.transport.JConfigGroup;
import org.jupiter.transport.JOption;

import java.util.Collections;
import java.util.List;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * Config for netty.
 *
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
        return getOptions(null, JOption.IO_RATIO);
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
        checkNotNull(option);

        if (option == JOption.IO_RATIO) {
            return (T) Integer.valueOf(getIoRatio());
        }
        return null;
    }

    @Override
    public <T> boolean setOption(JOption<T> option, T value) {
        validate(option, value);

        if (option == JOption.IO_RATIO) {
            setIoRatio(castToInteger(value));
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
        checkNotNull(option, "option");
        checkNotNull(value, "value");
    }

    private static Integer castToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof String) {
            return Integer.valueOf((String) value);
        }

        throw new IllegalArgumentException(value.getClass().toString());
    }

    private static Boolean castToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }

        throw new IllegalArgumentException(value.getClass().toString());
    }

    /**
     * TCP netty option
     */
    public static class NettyTcpConfigGroup implements JConfigGroup {

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
                checkNotNull(option);

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
                    setBacklog(castToInteger(value));
                } else if (option == JOption.SO_RCVBUF) {
                    setRcvBuf(castToInteger(value));
                } else if (option == JOption.SO_REUSEADDR) {
                    setReuseAddress(castToBoolean(value));
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
            private volatile int writeBufferHighWaterMark = -1;
            private volatile int writeBufferLowWaterMark = -1;
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
                        JOption.WRITE_BUFFER_HIGH_WATER_MARK,
                        JOption.WRITE_BUFFER_LOW_WATER_MARK,
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
                checkNotNull(option);

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
                if (option == JOption.WRITE_BUFFER_HIGH_WATER_MARK) {
                    return (T) Integer.valueOf(getWriteBufferHighWaterMark());
                }
                if (option == JOption.WRITE_BUFFER_LOW_WATER_MARK) {
                    return (T) Integer.valueOf(getWriteBufferLowWaterMark());
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
                    setRcvBuf(castToInteger(value));
                } else if (option == JOption.SO_SNDBUF) {
                    setSndBuf(castToInteger(value));
                } else if (option == JOption.SO_LINGER) {
                    setLinger(castToInteger(value));
                } else if (option == JOption.IP_TOS) {
                    setIpTos(castToInteger(value));
                } else if (option == JOption.CONNECT_TIMEOUT_MILLIS) {
                    setConnectTimeoutMillis(castToInteger(value));
                } else if (option == JOption.WRITE_BUFFER_HIGH_WATER_MARK) {
                    setWriteBufferHighWaterMark(castToInteger(value));
                } else if (option == JOption.WRITE_BUFFER_LOW_WATER_MARK) {
                    setWriteBufferLowWaterMark(castToInteger(value));
                } else if (option == JOption.SO_REUSEADDR) {
                    setReuseAddress(castToBoolean(value));
                } else if (option == JOption.KEEP_ALIVE) {
                    setKeepAlive(castToBoolean(value));
                } else if (option == JOption.TCP_NODELAY) {
                    setTcpNoDelay(castToBoolean(value));
                } else if (option == JOption.ALLOW_HALF_CLOSURE) {
                    setAllowHalfClosure(castToBoolean(value));
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

            public int getWriteBufferHighWaterMark() {
                return writeBufferHighWaterMark;
            }

            public void setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
                this.writeBufferHighWaterMark = writeBufferHighWaterMark;
            }

            public int getWriteBufferLowWaterMark() {
                return writeBufferLowWaterMark;
            }

            public void setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
                this.writeBufferLowWaterMark = writeBufferLowWaterMark;
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
}
