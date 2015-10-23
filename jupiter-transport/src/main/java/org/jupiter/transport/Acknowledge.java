package org.jupiter.transport;

/**
 * ACK确认
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class Acknowledge {

    public Acknowledge() {}

    public Acknowledge(long sequence) {
        this.sequence = sequence;
    }

    private long sequence; // ACK序号

    public long sequence() {
        return sequence;
    }

    public void sequence(long sequence) {
        this.sequence = sequence;
    }
}
