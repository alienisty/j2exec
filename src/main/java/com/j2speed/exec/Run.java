package com.j2speed.exec;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ METHOD })
public @interface Run {
   /**
    * Accepts strings like:
    * 
    * <pre>
    *    &lt;cmd&gt; ([&lt;option&gt;]['{?}'])*
    * </pre>
    * 
    * Example:
    * 
    * <pre>
    *    @Command("cmd -s {?} -d "a \"quoted\" srting" -g {?}")
    * </pre>
    * 
    * The string "{?}" is a binding parameter, similarly to the one found in JDBC. Like in JDBC they
    * are positional, so the first maps to the first parameter in the interface method, the second
    * to the second and so on.
    */
   String value();
}
