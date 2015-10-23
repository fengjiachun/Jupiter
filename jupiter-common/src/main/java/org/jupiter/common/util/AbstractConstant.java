package org.jupiter.common.util;

import org.jupiter.common.util.internal.UnsafeAccess;

import java.nio.ByteBuffer;

/**
 * Base implementation of {@link Constant}.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
public abstract class AbstractConstant<T extends AbstractConstant<T>> implements Constant<T> {

    private final int id;
    private final String name;
    private volatile long uniqueKey;

    public AbstractConstant(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    @SuppressWarnings("NullableProblems")
    public int compareTo(T o) {
        if (this == o) {
            return 0;
        }

        int returnCode = hashCode() - o.hashCode();
        if (returnCode != 0) {
            return returnCode;
        }

        @SuppressWarnings("UnnecessaryLocalVariable") AbstractConstant<T> other = o;
        long thisUK = uniqueKey();
        long otherUK = other.uniqueKey();
        if (thisUK < otherUK) {
            return -1;
        }
        if (thisUK > otherUK) {
            return 1;
        }

        throw new Error("failed to compare two different constants");
    }

    @Override
    public final boolean equals(Object obj) { // make it final, cannot be override
        return super.equals(obj);
    }

    @Override
    public final int hashCode() { // make it final, cannot be override
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Constant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    /**
     * Ensure return unique key value
     */
    private long uniqueKey() {
        long uniqueKey;
        if ((uniqueKey = this.uniqueKey) == 0) {
            synchronized (this) {
                while ((uniqueKey = this.uniqueKey) == 0) {
                    ByteBuffer directBuffer = ByteBuffer.allocateDirect(1);
                    this.uniqueKey = UnsafeAccess.directBufferAddress(directBuffer); // direct buf的内存地址是唯一的
                }
            }
        }
        return uniqueKey;
    }
}
