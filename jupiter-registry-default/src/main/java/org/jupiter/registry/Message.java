package org.jupiter.registry;

import java.util.concurrent.atomic.AtomicLong;

/**
 * jupiter
 * org.jupiter.registry
 *
 * @author jiachun.fjc
 */
public class Message {

    private static final AtomicLong sequenceGenerator = new AtomicLong(0);

    private final long sequence;
    private short sign;
    private long version; // 版本号
    private Object data;

    public Message() {
        this(sequenceGenerator.getAndIncrement());
    }

    public Message(long sequence) {
        this.sequence = sequence;
    }

    public long sequence() {
        return sequence;
    }

    public short sign() {
        return sign;
    }

    public void sign(short sign) {
        this.sign = sign;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Object data() {
        return data;
    }

    public void data(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sequence=" + sequence +
                ", sign=" + sign +
                ", version=" + version +
                ", data=" + data +
                '}';
    }
}
