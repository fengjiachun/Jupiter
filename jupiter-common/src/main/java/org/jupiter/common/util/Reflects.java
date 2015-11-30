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

import net.sf.cglib.reflect.FastClass;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.common.util.internal.UnsafeAccess.UNSAFE;

/**
 * Static utility methods pertaining to reflection.
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
     * Maps primitive {@link Class}es to their corresponding wrapper {@link Class}.
     */
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = Maps.newHashMap();

    static {
        primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
        primitiveWrapperMap.put(Byte.TYPE, Byte.class);
        primitiveWrapperMap.put(Character.TYPE, Character.class);
        primitiveWrapperMap.put(Short.TYPE, Short.class);
        primitiveWrapperMap.put(Integer.TYPE, Integer.class);
        primitiveWrapperMap.put(Long.TYPE, Long.class);
        primitiveWrapperMap.put(Double.TYPE, Double.class);
        primitiveWrapperMap.put(Float.TYPE, Float.class);
        primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
    }

    /**
     * Maps wrapper {@link Class}es to their corresponding primitive types.
     */
    private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap = Maps.newHashMap();

    static {
        for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperMap.entrySet()) {
            wrapperPrimitiveMap.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Array of primitive number types ordered by "promotability".
     */
    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES = {
            Byte.TYPE,
            Short.TYPE,
            Character.TYPE,
            Integer.TYPE,
            Long.TYPE,
            Float.TYPE,
            Double.TYPE
    };

    /**
     * Creates a new object without any constructor being called.
     *
     * @param clazz ths class to instantiate
     * @return new instance of clazz
     */
    public static <T> T newInstance(Class<T> clazz) {
        return objenesis.newInstance(clazz);
    }

    /**
     * Invokes the underlying method.
     *
     * @param obj            the object the underlying method is invoked from
     * @param methodName     the method name this object
     * @param parameterTypes the parameter types for the method this object
     * @param args           the arguments used for the method call
     * @return the result of dispatching the method represented by this object on {@code obj} with parameters
     */
    public static Object invoke(Object obj, String methodName, Class<?>[] parameterTypes, Object[] args) {
        Object value = null;
        try {
            Method method = obj.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            value = method.invoke(obj, args);
        } catch (Exception e) {
            UNSAFE.throwException(e);
        }
        return value;
    }

    /**
     * Invokes the underlying method, fast invoke using cglib's FastClass.
     *
     * @param obj            the object the underlying method is invoked from
     * @param methodName     the method name this object
     * @param parameterTypes the parameter types for the method this object
     * @param args           the arguments used for the method call
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
            UNSAFE.throwException(e);
        }
        return value;
    }

    /**
     * Returns a {@code Field} object that reflects the specified declared field
     * of the {@code Class} or interface represented by this {@code Class} object.
     * The {@code name} parameter is a {@code String} that specifies the
     * simple name of the desired field.
     *
     * @param clazz class
     * @param name  field name
     * @return the {@code Field} object for the specified field in this class
     * @throws NoSuchFieldException
     */
    public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> cls = checkNotNull(clazz, "class");
        while (cls != null) {
            try {
                return cls.getDeclaredField(name);
            } catch (Throwable ignored) {
            }

            cls = cls.getSuperclass();
        }
        throw new NoSuchFieldException(clazz.getName() + "#" + name);
    }

    /**
     * Returns the static value by name, on the specified {@code Class}. The value is
     * automatically wrapped in an object if it has a primitive type.
     *
     * @param clazz the specified class
     * @param name  the name of the represented field in class
     * @return the value of the represented field in class
     */
    public static Object getStaticValue(Class<?> clazz, String name) {
        Object value = null;
        try {
            Field fd = setAccessible(getField(clazz, name));
            value = fd.get(null);
        } catch (Exception e) {
            UNSAFE.throwException(e);
        }
        return value;
    }

    /**
     * Sets new value by name, on the specified {@code Class}. The new value is
     * automatically unwrapped if the underlying field has
     * a primitive type.
     *
     * @param clazz the specified class
     * @param name  the name of the the field in class
     * @param value the new value for the field in class
     */
    public static void setStaticValue(Class<?> clazz, String name, Object value) {
        try {
            Field fd = setAccessible(getField(clazz, name));
            fd.set(null, value);
        } catch (Exception e) {
            UNSAFE.throwException(e);
        }
    }

    /**
     * Returns the value by name, on the specified object. The value is
     * automatically wrapped in an object if it has a primitive type.
     *
     * @param o    the specified object
     * @param name the name of the represented field in object
     * @return the value of the represented field in object
     */
    public static Object getValue(Object o, String name) {
        Object value = null;
        try {
            Field fd = setAccessible(getField(o.getClass(), name));
            value = fd.get(o);
        } catch (Exception e) {
            UNSAFE.throwException(e);
        }
        return value;
    }

    /**
     * Sets new value by name, on the specified object. The new value
     * is automatically unwrapped if the underlying field has a primitive type.
     *
     * @param o     the specified object
     * @param name  the name of the the field in object
     * @param value the new value for the field in object
     */
    public static void setValue(Object o, String name, Object value) {
        try {
            Field fd = setAccessible(getField(o.getClass(), name));
            fd.set(o, value);
        } catch (Exception e) {
            UNSAFE.throwException(e);
        }
    }

    /**
     * Returns a proxy instance that implements {@code interfaceType} by dispatching
     * method invocations to {@code handler}. The class loader of {@code interfaceType}
     * will be used to define the proxy class. To implement multiple interfaces or
     * specify a class loader, use {@link Proxy#newProxyInstance}.
     *
     * @throws IllegalArgumentException if {@code interfaceType} does not specify
     *                                  the type of a Java interface
     */
    public static <T> T newProxy(Class<T> interfaceType, InvocationHandler handler) {
        checkNotNull(handler, "handler");
        checkArgument(interfaceType.isInterface(), interfaceType + " is not an interface");

        Object object = Proxy.newProxyInstance(
                interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, handler);

        return interfaceType.cast(object);
    }

    /**
     * Returns the default value for the specified class.
     */
    public static Object getTypeDefaultValue(Class<?> clazz) {
        checkNotNull(clazz, "clazz");

        if (clazz.isPrimitive()) {
            if (clazz == byte.class) {
                return (byte) 0;
            }
            if (clazz == short.class) {
                return (short) 0;
            }
            if (clazz == int.class) {
                return 0;
            }
            if (clazz == long.class) {
                return 0L;
            }
            if (clazz == float.class) {
                return 0F;
            }
            if (clazz == double.class) {
                return 0D;
            }
            if (clazz == char.class) {
                return (char) 0;
            }
            if (clazz == boolean.class) {
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
     * Generates a simplified name from a {@link Class}. Similar to {@link Class#getSimpleName()},
     * but it works fine with anonymous classes.
     */
    public static String simpleClassName(Class<?> clazz) {
        if (clazz == null) {
            return "null_class";
        }

        Package pkg = clazz.getPackage();
        return pkg == null ? clazz.getName() : clazz.getName().substring(pkg.getName().length() + 1);
    }

    /**
     * Find an array of parameter {@link Type}s that matches the given compatible parameters.
     */
    public static Class<?>[] findMatchingParameterTypes(List<Class<?>[]> parameterTypesList, Object[] args) {
        checkArgument(!parameterTypesList.isEmpty(), "empty parameterTypesList");

        if (parameterTypesList.size() == 1) {
            return parameterTypesList.get(0);
        }

        // 获取参数类型
        Class<?>[] parameterTypes;
        if (args == null || args.length == 0) {
            parameterTypes = new Class[0];
        } else {
            parameterTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i].getClass();
            }
        }

        Class<?>[] bestMatch = null;
        for (Class<?>[] pTypes : parameterTypesList) {
            if (isAssignable(parameterTypes, pTypes, true)) {
                if (bestMatch == null
                        || compareParameterTypes(pTypes, bestMatch, parameterTypes) < 0) {
                    bestMatch = pTypes;
                }
            }
        }

        return bestMatch;
    }

    /**
     * Checks if an array of {@link Class}es can be assigned to another array of {@link Class}es.
     */
    public static boolean isAssignable(Class<?>[] classArray, Class<?>[] toClassArray, final boolean autoboxing) {
        if (classArray.length != toClassArray.length) {
            return false;
        }

        for (int i = 0; i < classArray.length; i++) {
            if (!isAssignable(classArray[i], toClassArray[i], autoboxing)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if one {@link Class} can be assigned to a variable of another {@link Class}.
     */
    public static boolean isAssignable(Class<?> cls, final Class<?> toClass, final boolean autoboxing) {
        if (toClass == null) {
            return false;
        }

        // have to check for null, as isAssignableFrom doesn't
        if (cls == null) {
            return !(toClass.isPrimitive());
        }

        // autoboxing
        if (autoboxing) {
            if (cls.isPrimitive() && !toClass.isPrimitive()) {
                cls = primitiveToWrapper(cls);
                if (cls == null) {
                    return false;
                }
            }
            if (toClass.isPrimitive() && !cls.isPrimitive()) {
                cls = wrapperToPrimitive(cls);
                if (cls == null) {
                    return false;
                }
            }
        }

        if (cls.equals(toClass)) {
            return true;
        }

        // 对于原子类型, 根据JLS的规则进行扩展
        if (cls.isPrimitive()) {
            if (!toClass.isPrimitive()) {
                return false;
            }
            if (Boolean.TYPE.equals(cls)) {
                return false;
            }
            if (Integer.TYPE.equals(cls)) {
                return Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Long.TYPE.equals(cls)) {
                return Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Float.TYPE.equals(cls)) {
                return Double.TYPE.equals(toClass);
            }
            if (Double.TYPE.equals(cls)) {
                return false;
            }
            if (Character.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Short.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Byte.TYPE.equals(cls)) {
                return Short.TYPE.equals(toClass)
                        || Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            // should never get here
            return false;
        }

        return toClass.isAssignableFrom(cls);
    }

    /**
     * Converts the specified primitive {@link Class} object to its corresponding
     * wrapper Class object.
     */
    public static Class<?> primitiveToWrapper(final Class<?> cls) {
        Class<?> convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = primitiveWrapperMap.get(cls);
        }
        return convertedClass;
    }

    /**
     * Converts the specified wrapper {@link Class} to its corresponding primitive
     * class.
     */
    public static Class<?> wrapperToPrimitive(final Class<?> cls) {
        return wrapperPrimitiveMap.get(cls);
    }

    /**
     * Set the {@code accessible} flag for this object to the indicated boolean value.
     * A value of {@code true} indicates that the reflected object should suppress
     * Java language access checking when it is used.  A value of {@code false} indicates
     * that the reflected object should enforce Java language access checks.
     */
    private static Field setAccessible(Field fd) {
        if (!Modifier.isPublic(fd.getModifiers()) || !Modifier.isPublic(fd.getDeclaringClass().getModifiers())) {
            fd.setAccessible(true);
        }
        return fd;
    }

    /**
     * Compares the relative fitness of two sets of parameter types in terms of
     * matching a third set of runtime parameter types, such that a list ordered
     * by the results of the comparison would return the best match first
     * (least).
     *
     * @param left   the "left" parameter set
     * @param right  the "right" parameter set
     * @param actual the runtime parameter types to match against
     *               {@code left}/{@code right}
     * @return int consistent with {@code compare} semantics
     */
    private static int compareParameterTypes(Class<?>[] left, Class<?>[] right, Class<?>[] actual) {
        final float leftCost = getTotalTransformationCost(actual, left);
        final float rightCost = getTotalTransformationCost(actual, right);
        return leftCost < rightCost ? -1 : rightCost < leftCost ? 1 : 0;
    }

    /**
     * Returns the sum of the object transformation cost for each class in the
     * source argument list.
     *
     * @param srcArgs the source arguments
     * @param dstArgs the destination arguments
     * @return the total transformation cost
     */
    private static float getTotalTransformationCost(final Class<?>[] srcArgs, final Class<?>[] dstArgs) {
        float totalCost = 0.0f;
        for (int i = 0; i < srcArgs.length; i++) {
            Class<?> srcClass, dstClass;
            srcClass = srcArgs[i];
            dstClass = dstArgs[i];
            totalCost += getObjectTransformationCost(srcClass, dstClass);
        }
        return totalCost;
    }

    /**
     * Gets the number of steps required needed to turn the source class into
     * the destination class. This represents the number of steps in the object
     * hierarchy graph.
     *
     * @param srcClass the source class
     * @param dstClass the destination class
     * @return the cost of transforming an object
     */
    private static float getObjectTransformationCost(Class<?> srcClass, final Class<?> dstClass) {
        if (dstClass.isPrimitive()) {
            return getPrimitivePromotionCost(srcClass, dstClass);
        }
        float cost = 0.0f;
        while (srcClass != null && !dstClass.equals(srcClass)) {
            if (dstClass.isInterface() && isAssignable(srcClass, dstClass, true)) {
                // slight penalty for interface match.
                // we still want an exact match to override an interface match,
                // but
                // an interface match should override anything where we have to
                // get a superclass.
                cost += 0.25f;
                break;
            }
            cost++;
            srcClass = srcClass.getSuperclass();
        }
        /*
         * If the destination class is null, we've travelled all the way up to
         * an Object match. We'll penalize this by adding 1.5 to the cost.
         */
        if (srcClass == null) {
            cost += 1.5f;
        }
        return cost;
    }

    /**
     * Gets the number of steps required to promote a primitive number to another
     * type.
     *
     * @param srcClass the (primitive) source class
     * @param dstClass the (primitive) destination class
     * @return the cost of promoting the primitive
     */
    private static float getPrimitivePromotionCost(final Class<?> srcClass, final Class<?> dstClass) {
        float cost = 0.0f;
        Class<?> cls = srcClass;
        if (!cls.isPrimitive()) {
            // slight unwrapping penalty
            cost += 0.1f;
            cls = wrapperToPrimitive(cls);
        }
        for (int i = 0; cls != dstClass && i < ORDERED_PRIMITIVE_TYPES.length; i++) {
            if (cls == ORDERED_PRIMITIVE_TYPES[i]) {
                cost += 0.1f;
                if (i < ORDERED_PRIMITIVE_TYPES.length - 1) {
                    cls = ORDERED_PRIMITIVE_TYPES[i + 1];
                }
            }
        }
        return cost;
    }

    private Reflects() {}
}
