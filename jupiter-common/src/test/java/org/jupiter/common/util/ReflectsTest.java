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

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

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
    public void testNewInstance() {
        ReflectClass0 obj = Reflects.newInstance(ReflectClass0.class);
    }
}

class ReflectClass0 {

    public ReflectClass0() {
        System.out.println("ReflectClass");
    }

    public String method(String arg) {
        return "Hello " + arg;
    }
}
