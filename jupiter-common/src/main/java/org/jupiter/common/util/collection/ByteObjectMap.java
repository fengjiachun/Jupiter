package org.jupiter.common.util.collection;

import java.util.Map;

/**
 * Interface for a primitive map that uses {@code byte}s as keys.
 *
 * @param <V> the value type stored in the map.
 */
public interface ByteObjectMap<V> extends Map<Byte, V> {

    /**
     * A primitive entry in the map, provided by the iterator from {@link #entries()}
     *
     * @param <V> the value type stored in the map.
     */
    interface PrimitiveEntry<V> {
        /**
         * Gets the key for this entry.
         */
        byte key();

        /**
         * Gets the value for this entry.
         */
        V value();

        /**
         * Sets the value for this entry.
         */
        void setValue(V value);
    }

    /**
     * Gets the value in the map with the specified key.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value or {@code null} if the key was not found in the map.
     */
    V get(byte key);

    /**
     * Puts the given entry into the map.
     *
     * @param key   the key of the entry.
     * @param value the value of the entry.
     * @return the previous value for this key or {@code null} if there was no previous mapping.
     */
    V put(byte key, V value);

    /**
     * Removes the entry with the specified key.
     *
     * @param key the key for the entry to be removed from this map.
     * @return the previous value for the key, or {@code null} if there was no mapping.
     */
    V remove(byte key);

    /**
     * Gets an iterable to traverse over the primitive entries contained in this map. As an optimization,
     * the {@link PrimitiveEntry}s returned by the Iterator may change as the Iterator
     * progresses. The caller should not rely on {@link PrimitiveEntry} key/value stability.
     */
    Iterable<PrimitiveEntry<V>> entries();

    /**
     * Indicates whether or not this map contains a value for the specified key.
     */
    boolean containsKey(byte key);
}
