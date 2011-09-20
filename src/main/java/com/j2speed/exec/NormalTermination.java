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
 * <p>
 * Usually the value for this annotation is interpreted as an integer in base 10, an optional parser
 * class can be specified for using different formats.
 * 
 * @author Alessandro Nistico
 */
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface NormalTermination {
   /**
    * The value expected when the execution terminates normally
    */
   String value();

   /**
    * Specifies the class that contains a static method with the following signature:
    * 
    * <pre>
    * 
    * public static int parseInt(String s)
    * 
    * </pre>
    * 
    * By default the {@link Integer} class is used.
    */
   Class<?> parser() default Integer.class;
}
