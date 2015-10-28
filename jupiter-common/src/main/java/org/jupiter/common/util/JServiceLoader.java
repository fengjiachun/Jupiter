package org.jupiter.common.util;

import java.util.ServiceLoader;

/**
 * A simple service-provider loading facility (SPI).
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class JServiceLoader {

    public static <S> S load(Class<S> serviceClass) {
        return ServiceLoader.load(serviceClass).iterator().next();
    }
}
