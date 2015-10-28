package org.jupiter.registry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 带版本号的配置信息, 以保证消息乱序的情况下新数据不被老数据覆盖.
 *
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class ConfigWithVersion<T> {

    public static <T> ConfigWithVersion<T> newInstance() {
        return new ConfigWithVersion<>();
    }

    private ConfigWithVersion() {}

    private AtomicLong version = new AtomicLong(0);
    private T config;

    public long getVersion() {
        return version.get();
    }

    public long newVersion() {
        return version.incrementAndGet();
    }

    public T getConfig() {
        return config;
    }

    public void setConfig(T config) {
        this.config = config;
    }
}
