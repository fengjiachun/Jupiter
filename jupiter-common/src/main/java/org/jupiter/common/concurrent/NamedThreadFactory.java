package org.jupiter.common.concurrent;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * Named thread factory
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class NamedThreadFactory implements ThreadFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NamedThreadFactory.class);

    private static final AtomicInteger poolId = new AtomicInteger();

    private final AtomicInteger nextId = new AtomicInteger();
    private final String prefix;
    private final boolean daemon;
    private final ThreadGroup group;

    public NamedThreadFactory() {
        this("pool-" + poolId.incrementAndGet(), false);
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean daemon) {
        this.prefix = prefix + " #";
        this.daemon = daemon;
        SecurityManager s = System.getSecurityManager();
        group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Thread newThread(Runnable r) {
        checkNotNull(r, "runnable");

        String name = prefix + nextId.getAndIncrement();
        Thread t = new Thread(group, r, name, 0);
        try {
            if (t.isDaemon()) {
                if (!daemon) {
                    t.setDaemon(false);
                }
            } else {
                if (daemon) {
                    t.setDaemon(true);
                }
            }
        } catch (Exception ignored) { /* Doesn't matter even if failed to set. */ }

        logger.info("Creates new {}.", t);

        return t;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }
}
