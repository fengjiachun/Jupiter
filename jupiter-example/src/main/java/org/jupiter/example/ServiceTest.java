package org.jupiter.example;

import org.jupiter.rpc.annotation.ServiceProvider;

import java.util.List;

/**
 * jupiter
 * org.jupiter.example
 *
 * @author jiachun.fjc
 */
@ServiceProvider(group = "test", version = "1.0.0.daily")
public interface ServiceTest {

    ResultClass sayHello();

    class ResultClass {
        String str;
        int num;
        Long lon;
        List<String> list;

        @Override
        public String toString() {
            return "ResultClass{" +
                    "str='" + str + '\'' +
                    ", num=" + num +
                    ", lon=" + lon +
                    ", list=" + list +
                    '}';
        }
    }
}
