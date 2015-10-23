package org.jupiter.serialization;

/**
 * jupiter
 * org.jupiter.serialization
 *
 * @author jiachun.fjc
 */
public interface Serializer {

    <T> byte[] writeObject(T obj);

    <T> T readObject(byte[] bytes, Class<T> clazz);
}
