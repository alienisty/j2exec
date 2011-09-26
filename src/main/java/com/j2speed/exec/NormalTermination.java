package com.j2speed.exec;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to specify the exit value for normal termination of a command.
 * <p>
 * By default such value is {@code 0}, but a different value can be specified using this annotation.
 * 
 * @author Alessandro Nistico
 */
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface NormalTermination {
   /**
    * The value expected when the execution terminates normally
    */
   int value();
}
