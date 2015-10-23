package org.jupiter.serialization;

import org.jupiter.common.util.JServiceLoader;

/**
 * jupiter
 * org.jupiter.serialization
 *
 * @author jiachun.fjc
 */
public final class SerializerHolder {

    // SPI
    private static final Serializer serializer = JServiceLoader.load(Serializer.class);

    public static Serializer getSerializer() {
        return serializer;
    }
}
