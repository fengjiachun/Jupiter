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

package org.jupiter.example.zookeeper.spring;

import org.jupiter.example.ServiceTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 1.先启动外部的zookeeper
 * 2.再启动 SpringServer
 * 3.最后启动 SpringClient
 *
 * jupiter
 * org.jupiter.example.spring
 *
 * @author jiachun.fjc
 */
public class SpringJupiterClient {

    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:zk-spring-consumer.xml");
        ServiceTest service = ctx.getBean(ServiceTest.class);
        try {
            ServiceTest.ResultClass result1 = service.sayHello("jupiter");
            System.out.println(result1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
