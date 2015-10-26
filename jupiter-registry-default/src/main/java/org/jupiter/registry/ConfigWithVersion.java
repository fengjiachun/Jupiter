package org.jupiter.registry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 带版本号的配置信息
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
