package org.jupiter.example;

import java.util.Arrays;

/**
 * jupiter
 * org.jupiter.example
 *
 * @author jiachun.fjc
 */
public class ServiceTestImpl implements ServiceTest {

    @Override
    public ResultClass sayHello() {
        ResultClass result = new ResultClass();
        result.lon = 1L;
        result.num = 2;
        result.str = "Hello jupiter";
        result.list = Arrays.asList("H", "e", "l", "l", "o");
        return result;
    }
}
