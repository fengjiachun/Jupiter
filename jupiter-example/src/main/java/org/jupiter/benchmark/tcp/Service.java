package org.jupiter.benchmark.tcp;

import org.jupiter.rpc.annotation.ServiceProvider;

/**
 * jupiter
 * org.jupiter.benchmark.tcp
 *
 * @author jiachun.fjc
 */
@ServiceProvider
public interface Service {

    String hello(String arg);
}
