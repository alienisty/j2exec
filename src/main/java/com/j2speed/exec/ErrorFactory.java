package com.j2speed.exec;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to specify the {@link ErrorBuilderFactory} to be used. This can be applied globally on a
 * type and/or specifically on a method.
 * 
 * @author Alessandro Nistico
 */
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface ErrorFactory {
   Class<? extends ErrorBuilderFactory<?>> value();
}
