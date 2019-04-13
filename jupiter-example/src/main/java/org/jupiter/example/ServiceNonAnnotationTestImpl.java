/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jupiter.example;

import java.util.Arrays;
import java.util.List;

/**
 * jupiter
 * org.jupiter.example
 *
 * @author jiachun.fjc
 */
public class ServiceNonAnnotationTestImpl implements ServiceNonAnnotationTest {

    @Override
    public String sayHello(String arg1, Integer arg2, List<String> arg3) {
        return "arg1=" +
                arg1 +
                ", " +
                "arg2=" +
                arg2 +
                ", " +
                "arg3=" +
                arg3;
    }

    @Override
    public String sayHello2(String[] args) {
        return Arrays.toString(args);
    }
}
