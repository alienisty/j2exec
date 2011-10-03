package com.j2speed.exec;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation used to identify the environment parameter
 * 
 * @author Alessandro Nistico
 */
@Retention(RUNTIME)
@Target({ PARAMETER })
public @interface Env {
}
