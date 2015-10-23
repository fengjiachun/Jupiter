package org.jupiter.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.jupiter.common.util.JConstants.DEFAULT_GROUP;
import static org.jupiter.common.util.JConstants.DEFAULT_VERSION;

/**
 * jupiter
 * org.jupiter.rpc.annotation
 *
 * @author jiachun.fjc
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceProvider {

    String value() default "";

    String group() default DEFAULT_GROUP;

    String version() default DEFAULT_VERSION;
}
