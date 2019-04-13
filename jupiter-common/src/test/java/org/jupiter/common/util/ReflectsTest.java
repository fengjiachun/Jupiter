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
package org.jupiter.common.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class ReflectsTest {

    @Test
    public void testFastInvoke() throws InvocationTargetException {
        ReflectClass0 obj = new ReflectClass0();
        Object ret = Reflects.fastInvoke(obj, "method", new Class[] { String.class }, new Object[] { "Jupiter" });
        System.out.println(ret);
        assertThat(String.valueOf(ret), is("Hello Jupiter"));
    }

    @Test
    public void testFindMatchingMethod() {
        Map<String, List<Class<?>[]>> methodsParameterTypes = Maps.newHashMap();
        for (Method method : Service.class.getMethods()) {
            String methodName = method.getName();
            List<Class<?>[]> list = methodsParameterTypes.computeIfAbsent(methodName, k -> Lists.newArrayList());
            list.add(method.getParameterTypes());
        }

        List<Class<?>[]> list = methodsParameterTypes.get("method");
        Object[] args = new Object[] { 1, new StringBuilder("ss") };
        Class<?>[] parameterTypes = Reflects.findMatchingParameterTypes(list, args);
        Reflects.fastInvoke(new ServiceImpl(), "method", parameterTypes, args);
    }

    public static class ReflectClass0 {

        public ReflectClass0() {
            System.out.println("ReflectClass");
        }

        public String method(String arg) {
            return "Hello " + arg;
        }
    }

    public interface Service {

        void method(Integer i, String s);
        void method(Integer i, CharSequence s);
        void method(int i, String s);
        void method(int i, CharSequence s);
        void method(long i, CharSequence s);
    }

    public static class ServiceImpl implements Service {

        @Override
        public void method(Integer i, String s) {
            System.out.println("Integer i, String s");
        }

        @Override
        public void method(Integer i, CharSequence s) {
            System.out.println("Integer i, CharSequence s");
        }

        @Override
        public void method(int i, String s) {
            System.out.println("int i, String s");
        }

        @Override
        public void method(int i, CharSequence s) {
            System.out.println("int i, CharSequence s");
        }

        @Override
        public void method(long i, CharSequence s) {
            System.out.println("long i, CharSequence s");
        }
    }
}
