package org.jupiter.common.util;

import net.sf.cglib.reflect.FastClass;
import org.jupiter.common.util.internal.UnsafeAccess;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentMap;

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * Reflection tools.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class Reflects {

    private static final Objenesis objenesis = new ObjenesisStd(true);
    private static final ConcurrentMap<Class<?>, FastClass> fastClassCache = Maps.newConcurrentHashMap();

    /**
     * Creates a new object without any constructor being called
     *
     * @param clazz class to instantiate
     * @return new instance of clazz
     */
    public static <T> T newInstance(Class<T> clazz) {
        return objenesis.newInstance(clazz);
    }

    /**
     * Invokes the underlying method.
     *
     * @param obj the object the underlying method is invoked from
     * @param methodName the method name this object
     * @param parameterTypes the parameter types for the method this object
     * @param args the arguments used for the method call
     * @return the result of dispatching the method represented by this object on {@code obj} with parameters
     */
    public static Object invoke(Object obj, String methodName, Class<?>[] parameterTypes, Object[] args) {
        Object value = null;
        try {
            Method method = obj.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            value = method.invoke(obj, args);
        } catch (Exception e) {
            UnsafeAccess.UNSAFE.throwException(e);
        }
        return value;
    }

    /**
     * Invokes the underlying method, fast invoke using cglib's FastClass.
     *
     * @param obj the object the underlying method is invoked from
     * @param methodName the method name this object
     * @param parameterTypes the parameter types for the method this object
     * @param args the arguments used for the method call
     * @return the result of dispatching the method represented by this object on {@code obj} with parameters
     */
    public static Object fastInvoke(Object obj, String methodName, Class<?>[] parameterTypes, Object[] args) {
        Class<?> clazz = obj.getClass();
        FastClass fastClass = fastClassCache.get(clazz);
        if (fastClass == null) {
            FastClass newFastClass = FastClass.create(clazz);
            fastClass = fastClassCache.putIfAbsent(clazz, newFastClass);
            if (fastClass == null) {
                fastClass = newFastClass;
            }
        }

        Object value = null;
        try {
            value = fastClass.invoke(methodName, parameterTypes, obj, args);
        } catch (InvocationTargetException e) {
            UnsafeAccess.UNSAFE.throwException(e);
        }
        return value;
    }

    /**
     * Returns a {@code Field} object that reflects the specified declared field
     * of the class or interface represented by this {@code Class} object.
     * The {@code name} parameter is a {@code String} that specifies the
     * simple name of the desired field.
     *
     * @param clazz class
     * @param name field name
     * @return the {@code Field} object for the specified field in this class
     * @throws NoSuchFieldException
     */
    public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> cls = checkNotNull(clazz, "class");
        while (cls != null) {
            try {
                return cls.getDeclaredField(name);
            } catch (Throwable ignored) {}

            cls = cls.getSuperclass();
        }
        throw new NoSuchFieldException(clazz.getName() + "#" + name);
    }

    /**
     * Returns the value by name, on the specified object.
     * The value is automatically wrapped in an object if it has a primitive type.
     *
     * @param o the specified object
     * @param name the name of the represented field in object
     * @return the value of the represented field in object
     */
    public static Object getValue(Object o, String name) {
        Object value = null;
        try {
            Field fd = setAccessible(getField(o.getClass(), name));
            value = fd.get(o);
        } catch (Exception e) {
            UnsafeAccess.UNSAFE.throwException(e);
        }
        return value;
    }

    /**
     * Sets new value by name, on the specified object.
     * The new value is automatically unwrapped if the underlying field
     * has a primitive type.
     *
     * @param o the specified object
     * @param name the name of the the field in object
     * @param value the new value for the field in object
     */
    public static void setValue(Object o, String name, Object value) {
        try {
            Field fd = setAccessible(getField(o.getClass(), name));
            fd.set(o, value);
        } catch (Exception e) {
            UnsafeAccess.UNSAFE.throwException(e);
        }
    }

    /**
     * Returns the class loader for this class.
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader cl = clazz.getClassLoader();
        if (cl != null) {
            return cl;
        }

        cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return cl;
        }

        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Returns the default value for this class.
     */
    public static Object getTypeDefaultValue(Class<?> clazz) {
        checkNotNull(clazz, "clazz");

        if (clazz.isPrimitive()) {
            if (byte.class == clazz) {
                return (byte) 0;
            }
            if (short.class == clazz) {
                return (short) 0;
            }
            if (int.class == clazz) {
                return 0;
            }
            if (long.class == clazz) {
                return 0L;
            }
            if (float.class == clazz) {
                return 0F;
            }
            if (double.class == clazz) {
                return 0D;
            }
            if (char.class == clazz) {
                return (char) 0;
            }
            if (boolean.class == clazz) {
                return false;
            }
        }
        return null;
    }

    /**
     * The shortcut to {@link #simpleClassName(Class) simpleClassName(o.getClass())}.
     */
    public static String simpleClassName(Object o) {
        return o == null ? "null_object" : simpleClassName(o.getClass());
    }

    /**
     * Generates a simplified name from a {@link Class}.
     * Similar to {@link Class#getSimpleName()}, but it works fine with anonymous classes.
     */
    public static String simpleClassName(Class<?> clazz) {
        if (clazz == null) {
            return "null_class";
        }

        Package pkg = clazz.getPackage();
        return pkg == null ? clazz.getName() : clazz.getName().substring(pkg.getName().length() + 1);
    }

    private static Field setAccessible(Field fd) {
        if (!Modifier.isPublic(fd.getModifiers()) || !Modifier.isPublic(fd.getDeclaringClass().getModifiers())) {
            fd.setAccessible(true);
        }
        return fd;
    }

    private Reflects() {}
}
