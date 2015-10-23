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
