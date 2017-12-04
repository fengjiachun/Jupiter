package org.jupiter.example.filter;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.rpc.JFilter;

/**
 * jupiter
 * org.jupiter.example.filter
 *
 * @author jiachun.fjc
 */
public class OpenTracingTest {

    public static void main(String[] args) {
        System.out.println(JServiceLoader.load(JFilter.class).iterator().next());
    }
}
