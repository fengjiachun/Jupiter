package org.jupiter.rpc;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class BytesHolder {

    private transient byte[] bytes;

    public byte[] bytes() {
        return bytes;
    }

    public void bytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
