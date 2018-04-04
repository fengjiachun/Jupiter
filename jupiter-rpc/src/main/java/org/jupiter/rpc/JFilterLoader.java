package org.jupiter.rpc;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.List;

import static org.jupiter.common.util.StackTraceUtil.stackTrace;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JFilterLoader {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JFilterLoader.class);

    public static JFilterChain loadExtFilters(JFilterChain chain, JFilter.Type type) {
        try {
            List<JFilter> sortedList = JServiceLoader.load(JFilter.class).sort();

            // 优先级高的在队首
            for (int i = sortedList.size() - 1; i >= 0; i--) {
                JFilter extFilter = sortedList.get(i);
                JFilter.Type extType = extFilter.getType();
                if (extType == type || extType == JFilter.Type.ALL) {
                    chain = new DefaultFilterChain(extFilter, chain);
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to load extension filters: {}.", stackTrace(t));
        }

        return chain;
    }
}
