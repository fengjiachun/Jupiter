package org.jupiter.benchmark.tcp;

/**
 * jupiter
 * org.jupiter.benchmark.tcp
 *
 * @author jiachun.fjc
 */
public class ServiceImpl implements Service {

    @Override
    public String hello(String arg) {
        return "Hello " + arg;
    }
}
