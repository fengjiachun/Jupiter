package org.jupiter.rpc;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.SpiImpl;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.util.Collections;
import java.util.Comparator;
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
            List<JFilter> extFilters = Lists.newArrayList(JServiceLoader.load(JFilter.class));

            Collections.sort(extFilters, new Comparator<JFilter>() {

                @Override
                public int compare(JFilter o1, JFilter o2) {
                    SpiImpl o1_spi = o1.getClass().getAnnotation(SpiImpl.class);
                    SpiImpl o2_spi = o2.getClass().getAnnotation(SpiImpl.class);

                    int o1_sequence = o1_spi == null ? 0 : o1_spi.sequence();
                    int o2_sequence = o2_spi == null ? 0 : o2_spi.sequence();

                    return o1_sequence - o2_sequence;
                }
            });

            // sequence越大越靠前
            for (JFilter f : extFilters) {
                JFilter.Type fType = f.getType();
                if (fType == JFilter.Type.ALL || type.equals(fType)) {
                    chain = new DefaultFilterChain(f, chain);
                }
            }
        } catch (Throwable t) {
            logger.warn("Failed to load extension filters: {}.", stackTrace(t));
        }

        return chain;
    }
}
